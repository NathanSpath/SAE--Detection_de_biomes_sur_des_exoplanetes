import AlgoCluster.DBSCAN;
import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import flou.Flou;
import flou.FlouMoyenne;
import palette.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainDBSCAN {

    // Seuil de similarité pour le filtrage des couleurs de la palette.
    static final double SEUIL_SIMILARITE = 15.0;
    // Nombre de couleurs à extraire pour la palette initiale.
    static final int NB_COULEURS_PALETTE = 15;

    public static void main(String[] args) throws IOException {
        System.out.println("--- Démarrage du pipeline de détection de biomes et écosystèmes ---");

        // 1. Chargement et pré-traitement de l'image
        File inputFile = new File("Images/Planete_1.jpg");
        if (!inputFile.exists()) {
            System.out.println("Erreur : Fichier introuvable.");
            return;
        }
        BufferedImage originalImage = ImageIO.read(inputFile);
        int totalPixels = originalImage.getWidth() * originalImage.getHeight();

        // Appliquer un léger flou pour réduire le bruit et homogénéiser les zones
        Flou flou = new FlouMoyenne(3);
        BufferedImage blurredImg = flou.appliquerFlou(inputFile);
        System.out.println("Image pré-traitée avec un filtre de flou.");

        // --- PARTIE 1: DÉTECTION DES BIOMES ---

        // 2. Génération de la palette de couleurs des biomes
        NormeCouleurs norme = new NormeBetterCIELAB();
        AlgoExtractionPalette kmeans = new PaletteKmeans(NB_COULEURS_PALETTE);
        System.out.println("Extraction de " + NB_COULEURS_PALETTE + " couleurs candidates...");
        Color[] couleursCandidates = kmeans.extrairePalette(blurredImg, NB_COULEURS_PALETTE, norme);
        Color[] paletteFinale = filtrerCouleursUniques(couleursCandidates, norme, SEUIL_SIMILARITE);
        Palette paletteBiomes = new Palette(new BiomeMapper(norme).getBiomeMapping(paletteFinale), norme);
        System.out.println("Palette de biomes finale créée avec " + paletteBiomes.getNbBiomes() + " couleurs.");

        // 3. Création de la carte des biomes et regroupement des pixels par biome
        System.out.println("Création de la carte des biomes et regroupement des pixels...");
        BufferedImage biomeMapImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Map<Color, List<Point>> pixelsParBiome = new HashMap<>();
        for (Color biomeColor : paletteBiomes.getBiomeColors().values()) {
            pixelsParBiome.put(biomeColor, new ArrayList<>());
        }

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                Color pixelColor = new Color(blurredImg.getRGB(x, y));
                String biomeName = paletteBiomes.getBiomePlusProche(pixelColor);
                Color biomeColor = paletteBiomes.getBiomeColors().get(biomeName);

                biomeMapImage.setRGB(x, y, biomeColor.getRGB());
                pixelsParBiome.get(biomeColor).add(new Point(x, y));
            }
        }
        new File("imagesBiomes").mkdirs();
        ImageIO.write(biomeMapImage, "PNG", new File("imagesBiomes/rendu_carte_biomes.png"));
        System.out.println("Carte des biomes sauvegardée dans 'imagesBiomes/rendu_carte_biomes.png'");


        // --- PARTIE 2: DÉTECTION DES ÉCOSYSTÈMES ---

        System.out.println("\nDétection des écosystèmes pour chaque biome...");
        BufferedImage ecosystemMapImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = ecosystemMapImage.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());

        double epsForEcosystems = 3.0;
        int minPtsForEcosystems = 20;
        DBSCAN dbscan = new DBSCAN(epsForEcosystems, minPtsForEcosystems);
        Random rand = new Random();

        for (Map.Entry<Color, List<Point>> entry : pixelsParBiome.entrySet()) {
            Color biomeColor = entry.getKey();
            List<Point> biomePixels = entry.getValue();

            // Heuristique pour identifier et exclure les océans du clustering spatial
            boolean isVeryDark = (biomeColor.getRed() + biomeColor.getGreen() + biomeColor.getBlue()) < 60;
            boolean isVeryLarge = biomePixels.size() > totalPixels * 0.30;

            if (isVeryDark && isVeryLarge) {
                System.out.println("Exclusion du biome de type océan (trop grand) : " + biomePixels.size() + " pixels.");
                // On dessine l'océan avec sa couleur de base, sans chercher les écosystèmes
                for (Point p : biomePixels) {
                    ecosystemMapImage.setRGB(p.x, p.y, biomeColor.getRGB());
                }
                continue; // On passe au biome suivant
            }

            if (biomePixels.size() < minPtsForEcosystems) {
                continue;
            }

            System.out.println("Traitement du biome (couleur RGB " + biomeColor.getRed() + "," + biomeColor.getGreen() + "," + biomeColor.getBlue() + ") contenant " + biomePixels.size() + " pixels.");

            double[][] points = new double[biomePixels.size()][2];
            for (int i = 0; i < biomePixels.size(); i++) {
                points[i][0] = biomePixels.get(i).getX();
                points[i][1] = biomePixels.get(i).getY();
            }

            int[] labels = dbscan.cluster(points);

            Map<Integer, Color> ecosystemColors = new HashMap<>();
            int ecosystemCount = 0;
            for (int i = 0; i < labels.length; i++) {
                int clusterId = labels[i];
                if (clusterId == 0) continue;

                if (!ecosystemColors.containsKey(clusterId)) {
                    ecosystemColors.put(clusterId, new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                    ecosystemCount++;
                }

                Point p = biomePixels.get(i);
                ecosystemMapImage.setRGB(p.x, p.y, ecosystemColors.get(clusterId).getRGB());
            }
            if (ecosystemCount > 0) {
                System.out.println(" -> " + ecosystemCount + " écosystèmes détectés.");
            }
        }
        g2d.dispose();
        ImageIO.write(ecosystemMapImage, "PNG", new File("imagesBiomes/rendu_ecosystemes.png"));
        System.out.println("\nCarte des écosystèmes sauvegardée dans 'imagesBiomes/rendu_ecosystemes.png'");
        System.out.println("--- Pipeline terminé ---");
    }

    private static Color[] filtrerCouleursUniques(Color[] couleursEntrantes, NormeCouleurs norme, double seuil) {
        List<Color> couleursUniques = new ArrayList<>();
        for (Color couleurCandidate : couleursEntrantes) {
            boolean estTropSimilaire = couleursUniques.stream()
                .anyMatch(couleur -> norme.distanceCouleur(couleurCandidate, couleur) < seuil);
            if (!estTropSimilaire) {
                couleursUniques.add(couleurCandidate);
            }
        }
        return couleursUniques.toArray(new Color[0]);
    }
}
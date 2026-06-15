import AlgoCluster.DBSCAN;
import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import flou.Flou;
import flou.FlouMoyenne;
import palette.BiomeMapper;
import palette.ExtractionPalette;
import palette.Palette;
import palette.PaletteKmeans;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainDBSCAN {
    public static void main(String[] args)  {
        try {
            System.out.println("--- Démarrage du pipeline de détection de biomes ---");

            // 1. Chargement de l'image source
            File inputFile = new File("Images/Planete_1.jpg");
            if (!inputFile.exists()) {
                System.out.println("Erreur : Fichier introuvable.");
                return;
            }
            BufferedImage originalImage = ImageIO.read(inputFile);

            // Store original dimensions for final output
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();


            // 3. Application du Flou
            System.out.println("Application du filtre de flou...");

            Flou flou = new FlouMoyenne(3);
            BufferedImage blurredImg = flou.appliquerFlou(inputFile);

            // 4. Extraction de la palette de couleurs (K-Means)
            System.out.println("Extraction de la palette (K-Means)...");
            NormeCouleurs norme = new NormeBetterCIELAB();
            ExtractionPalette kmeans = new PaletteKmeans();
            int nbBiomesAttendus = 20;
            Color[] paletteDominante = kmeans.extrairePalette(blurredImg, nbBiomesAttendus, norme);

            Color[] paletteFinale = filtrerCouleursUniques(paletteDominante, norme, 15.0);

            BiomeMapper mapper = new BiomeMapper(norme);
            Map<String, Color> paletteBiome = mapper.getBiomeMapping(paletteFinale);


            // 5. Génération de la carte des biomes
            System.out.println("Génération de la carte des biomes...");
            BufferedImage biomeMap = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < originalHeight; y++) {
                for (int x = 0; x < originalWidth; x++) {

                    // Récupérer la couleur du pixel original
                    Color pixelColor = new Color(blurredImg.getRGB(x, y));

                    // Trouver la couleur de biome la plus proche
                    Color closestColor = null;
                    double minDistance = Double.MAX_VALUE;

                    // On calcule la distance entre la couleur du pixel avec celle de chaque couleurs de la palette
                    for (Map.Entry<String, Color> entry : paletteBiome.entrySet()) {
                        Color biomeColor = entry.getValue();

                        double distance = norme.distanceCouleur(pixelColor, biomeColor);

                        if (distance < minDistance) {
                            minDistance = distance;
                            closestColor = biomeColor;
                        }
                    }

                    // Colorier le pixel
                    biomeMap.setRGB(x, y, closestColor.getRGB());
                }
            }

            // 6. Sauvegarde
            //File biomeMapFile = new File("carte_biomes.png");
            //ImageIO.write(biomeMap, "PNG", biomeMapFile);

            // 6. Sauvegarde de la carte complète
            File biomeMapFile = new File("carte_biomes.png");
            ImageIO.write(biomeMap, "PNG", biomeMapFile);

            // ==========================================
            // 7. Visualisation de Biome Individuel (Bonus)
            // ==========================================
            System.out.println("Génération du calque pour un biome spécifique...");

            // On vérifie qu'on a bien trouvé des biomes
            if (!paletteBiome.isEmpty()) {
                // Pour l'exemple, on prend le tout premier biome de ta HashMap
                Map.Entry<String, Color> biomeCible = paletteBiome.entrySet().iterator().next();
                String nomBiome = biomeCible.getKey();
                Color couleurBiome = biomeCible.getValue();

                System.out.println("Isolement en cours pour le biome : " + nomBiome);

                // On nettoie le nom du fichier au cas où il y aurait des espaces
                String nomFichierPropre = nomBiome.replaceAll("\\s+", "_");
                String fichierSortie = "masque_biome_" + nomFichierPropre + ".png";

                // On appelle notre nouvelle méthode
                genererMasqueBiome(originalImage, biomeMap, couleurBiome, fichierSortie);
                System.out.println("Calque généré avec succès : " + fichierSortie);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<Integer, Color> getClusterBiomeColors(int[] labels, Color[] pixelColors) {
        Map<Integer, Map<Color, Integer>> clusterColorCounts = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            int clusterId = labels[i];
            if (clusterId != 0) { // Exclure le bruit
                Color pixelBiomeColor = pixelColors[i];
                clusterColorCounts.computeIfAbsent(clusterId, k -> new HashMap<>())
                                  .merge(pixelBiomeColor, 1, Integer::sum);
            }
        }

        Map<Integer, Color> clusterBiomeColors = new HashMap<>();
        for (Map.Entry<Integer, Map<Color, Integer>> entry : clusterColorCounts.entrySet()) {
            int clusterId = entry.getKey();
            entry.getValue().entrySet().stream()
                 .max(Map.Entry.comparingByValue())
                 .ifPresent(dominantEntry -> clusterBiomeColors.put(clusterId, dominantEntry.getKey()));
        }
        return clusterBiomeColors;
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

    /**
     * Génère une image isolant un biome spécifique.
     * Les pixels n'appartenant pas au biome deviennent transparents.
     */
    private static void genererMasqueBiome(BufferedImage imageOriginale, BufferedImage carteBiomes, Color couleurBiomeCible, String nomFichierSortie) throws IOException {
        int width = imageOriginale.getWidth();
        int height = imageOriginale.getHeight();

        // On utilise ARGB pour gérer le canal Alpha (la transparence)
        BufferedImage masqueImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Couleur transparente (Noir avec 0 d'opacité)
        int couleurTransparente = new Color(0, 0, 0, 0).getRGB();
        int cibleRGB = couleurBiomeCible.getRGB();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // On regarde à quel biome appartient ce pixel sur la carte générée
                int pixelBiomeCourant = carteBiomes.getRGB(x, y);

                if (pixelBiomeCourant == cibleRGB) {
                    // Si c'est le bon biome, on affiche le pixel de l'image source (ou floutée)
                    masqueImg.setRGB(x, y, imageOriginale.getRGB(x, y));
                } else {
                    // Sinon, on rend le pixel invisible
                    masqueImg.setRGB(x, y, couleurTransparente);
                }
            }
        }

        // Sauvegarde de l'image isolée
        ImageIO.write(masqueImg, "PNG", new File(nomFichierSortie));
    }
}
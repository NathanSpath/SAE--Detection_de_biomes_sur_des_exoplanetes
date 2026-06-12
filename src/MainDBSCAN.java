import AlgoCluster.DBSCAN;
import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import Norme.NormeRedmean;
import flou.Flou;
import flou.FlouGausien;
import flou.FlouMoyenne;
import palette.BiomeMapper;
import palette.ExtractionPalette;
import palette.Palette;
import palette.PaletteKmeans;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

public class MainDBSCAN {
    public static void main(String[] args)  {
        try {
            System.out.println("--- Démarrage du pipeline de détection de biomes ---");

            // 1. Chargement de l'image source
            File inputFile = new File("Images/Planete_1.jpg");
            if (!inputFile.exists()) {
                System.out.println("Erreur : Fichier  introuvable.");
                return;
            }
            BufferedImage originalImage = ImageIO.read(inputFile);

            // 2. Redimensionnement (Crucial pour la complexité O(N^2) de DBSCAN)
            int width = 60;
            int height = 60;
            Image tmp = originalImage.getScaledInstance(width, height, Image.SCALE_FAST);
            BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImg.createGraphics();
            g2d.drawImage(tmp, 0, 0, null);
            g2d.dispose();

            // 3. Application du Flou Gaussien (pour uniformiser les zones)
            System.out.println("Application du filtre gaussien...");
            // Ton constructeur prend un File, on sauvegarde donc temporairement l'image réduite
            File tempFile = new File("temp_resized.png");
            ImageIO.write(resizedImg, "PNG", tempFile);

            Flou flou = new FlouMoyenne(5);
            BufferedImage blurredImg = flou.appliquerFlou(tempFile);
            tempFile.delete(); // Nettoyage du fichier temporaire

            // 4. Extraction K-Means
            System.out.println("Extraction de la palette (K-Means)...");
            NormeCouleurs norme = new NormeBetterCIELAB();
            ExtractionPalette kmeans = new PaletteKmeans();
            int nbBiomesAttendus = 20;
            Color[] paletteDominante = kmeans.extrairePalette(blurredImg, nbBiomesAttendus, norme);

            Color[] paletteFinale = filtrerCouleursUniques(paletteDominante, norme, Main.SEUIL_SIMILARITE);

            BiomeMapper mapper = new BiomeMapper(norme);
            Map<String, Color> paletteBiome = mapper.getBiomeMapping(paletteFinale);


            // Préparation des données pour DBSCAN : tableau 2D [x, y, R, G, B]
            int numPixels = width * height;
            double[][] X = new double[numPixels][5];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color originalColor = new Color(blurredImg.getRGB(x, y));
                    Color nearestColor = new Palette((Color[]) paletteBiome.keySet().toArray(),norme).getPlusProche(originalColor);

                    int index = y * width + x;
                    X[index][0] = x;
                    X[index][1] = y;
                    X[index][2] = nearestColor.getRed();
                    X[index][3] = nearestColor.getGreen();
                    X[index][4] = nearestColor.getBlue();
                }
            }

            // 6. Lancement de DBSCAN
            System.out.println("Lancement de DBSCAN sur " + numPixels + " pixels...");
            // eps = 1.5 permet de lier un pixel à ses voisins directs (haut/bas/gauche/droite + diagonales)
            // minPts = 4 signifie qu'un biome doit faire au moins 4 pixels de large pour être valide
            DBSCAN dbscan = new DBSCAN(1.5, 4);

            long start = System.currentTimeMillis();
            int[] labels = dbscan.cluster(X);
            long end = System.currentTimeMillis();
            System.out.println("DBSCAN terminé en " + (end - start) + " ms.");

            // 7. Génération du rendu final
            BufferedImage resultImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Map<Integer, Color> clusterColors = new HashMap<>();
            Random rand = new Random();

            for (int i = 0; i < labels.length; i++) {
                int x = i % width;
                int y = i / width;
                int clusterId = labels[i];

                if (clusterId == 0) { // Cluster 0 = Bruit selon ta classe DBSCAN
                    resultImg.setRGB(x, y, Color.BLACK.getRGB());
                } else {
                    // Chaque zone connexe reçoit une couleur aléatoire unique pour bien les différencier visuellement
                    if (!clusterColors.containsKey(clusterId)) {
                        clusterColors.put(clusterId, new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                    }
                    resultImg.setRGB(x, y, clusterColors.get(clusterId).getRGB());
                }
            }

            // 8. Sauvegarde
            File outputFile = new File("rendu_zones_biomes.png");
            ImageIO.write(resultImg, "PNG", outputFile);
            System.out.println("Terminé ! " + clusterColors.size() + " zones (clusters) uniques détectées.");
            System.out.println("Image sauvegardée : " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static Color[] filtrerCouleursUniques(Color[] couleursEntrantes, NormeCouleurs norme, double seuil) {
        List<Color> couleursUniques = new ArrayList<>();

        for (Color couleurCandidate : couleursEntrantes) {
            boolean estTropSimilaire = false;
            for (Color couleurDejaAjoutee : couleursUniques) {
                if (norme.distanceCouleur(couleurCandidate, couleurDejaAjoutee) < seuil) {
                    estTropSimilaire = true;
                    break;
                }
            }

            if (!estTropSimilaire) {
                couleursUniques.add(couleurCandidate);
            }
        }

        return couleursUniques.toArray(new Color[0]);
    }
}

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

            // 2. Redimensionnement pour l'analyse - Augmentation pour plus de détails
            int resizedWidth = 100;
            int resizedHeight = 100;
            Image tmp = originalImage.getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_SMOOTH);
            BufferedImage resizedImg = new BufferedImage(resizedWidth, resizedHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImg.createGraphics();
            g2d.drawImage(tmp, 0, 0, null);
            g2d.dispose();

            // 3. Application du Flou
            System.out.println("Application du filtre de flou...");
            File tempFile = new File("temp_resized.png");
            ImageIO.write(resizedImg, "PNG", tempFile);

            Flou flou = new FlouMoyenne(3);
            BufferedImage blurredImg = flou.appliquerFlou(tempFile);
            tempFile.delete();

            // 4. Extraction de la palette de couleurs (K-Means)
            System.out.println("Extraction de la palette (K-Means)...");
            NormeCouleurs norme = new NormeBetterCIELAB();
            ExtractionPalette kmeans = new PaletteKmeans();
            int nbBiomesAttendus = 20;
            Color[] paletteDominante = kmeans.extrairePalette(blurredImg, nbBiomesAttendus, norme);

            Color[] paletteFinale = filtrerCouleursUniques(paletteDominante, norme, 15.0);

            BiomeMapper mapper = new BiomeMapper(norme);
            Map<String, Color> paletteBiome = mapper.getBiomeMapping(paletteFinale);

            // 5. Préparation des données pour DBSCAN avec normalisation des coordonnées
            int numPixelsResized = resizedWidth * resizedHeight;
            double[][] X = new double[numPixelsResized][5];
            Color[] pixelNearestColors = new Color[numPixelsResized];

            for (int y = 0; y < resizedHeight; y++) {
                for (int x = 0; x < resizedWidth; x++) {
                    Color pixelColor = new Color(blurredImg.getRGB(x, y));
                    String biome = new Palette(paletteBiome, norme).getBiomePlusProche(pixelColor);
                    Color nearestColor = paletteBiome.get(biome);

                    int index = y * resizedWidth + x;
                    // Normalisation des coordonnées pour les ramener à une échelle similaire à celle des couleurs (0-255)
                    X[index][0] = x * (255.0 / resizedWidth);
                    X[index][1] = y * (255.0 / resizedHeight);
                    X[index][2] = nearestColor.getRed();
                    X[index][3] = nearestColor.getGreen();
                    X[index][4] = nearestColor.getBlue();
                    pixelNearestColors[index] = nearestColor;
                }
            }

            // 6. Lancement de DBSCAN
            System.out.println("Lancement de DBSCAN sur " + numPixelsResized + " pixels...");
            // Les paramètres peuvent nécessiter un ajustement avec la nouvelle résolution et la normalisation
            DBSCAN dbscan = new DBSCAN(5, 5);
            long start = System.currentTimeMillis();
            int[] labels = dbscan.cluster(X);
            long end = System.currentTimeMillis();
            System.out.println("DBSCAN terminé en " + (end - start) + " ms.");

            // 7. Déterminer la couleur de biome dominante pour chaque cluster
            Map<Integer, Color> clusterBiomeColors = getClusterBiomeColors(labels, pixelNearestColors);

            // 8. Génération du rendu final en haute résolution
            System.out.println("Génération de l'image finale en haute résolution...");
            BufferedImage resultImg = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < originalHeight; y++) {
                for (int x = 0; x < originalWidth; x++) {
                    int rx = (int) (x * ((double) resizedWidth / originalWidth));
                    int ry = (int) (y * ((double) resizedHeight / originalHeight));
                    int resizedIndex = Math.min(ry, resizedHeight - 1) * resizedWidth + Math.min(rx, resizedWidth - 1);

                    int clusterId = labels[resizedIndex];

                    if (clusterId == 0) { // Bruit
                        resultImg.setRGB(x, y, Color.BLACK.getRGB());
                    } else {
                        // Appliquer la couleur de biome solide pour une différenciation claire
                        Color biomeColor = clusterBiomeColors.get(clusterId);
                        if (biomeColor != null) {
                            resultImg.setRGB(x, y, biomeColor.getRGB());
                        } else {
                            resultImg.setRGB(x, y, Color.MAGENTA.getRGB()); // Fallback
                        }
                    }
                }
            }

            // 9. Sauvegarde
            File outputFile = new File("imagesBiomes/rendu_zones_biomes.png");
            ImageIO.write(resultImg, "PNG", outputFile);
            System.out.println("Terminé ! " + clusterBiomeColors.size() + " zones (clusters) uniques détectées.");
            System.out.println("Image sauvegardée : " + outputFile.getAbsolutePath());

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
}
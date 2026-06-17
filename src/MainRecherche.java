import AlgoCluster.AlgoClustering;
import AlgoCluster.DBSCAN;
import AlgoCluster.HAC;
import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import flou.Flou;
import flou.FlouMoyenne;
import palette.AlgoExtractionPalette;
import palette.BiomeMapper;
import palette.Palette;
import palette.PaletteKmeans;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MainRecherche {

    // --- CONFIGURATION DES TESTS ---

    // Paramètres à tester pour le flou
    private static final int[] FLOU_RAYONS = {3, 5};

    // Paramètres à tester pour K-Means
    private static final int[] KMEANS_K_VALUES = {10, 15, 20};

    // Paramètres à tester pour DBSCAN
    private static final double[] DBSCAN_EPS_VALUES = {3.0, 5.0};
    private static final int[] DBSCAN_MIN_PTS_VALUES = {20, 50};

    // Paramètres à tester pour HAC (placeholder)
    private static final int[] HAC_K_VALUES = {10, 20};

    // Seuil de pixels pour ignorer le clustering d'un biome
    private static final int PIXEL_THRESHOLD_FOR_ECOSYSTEM_CLUSTERING = 250_000;


    public static void main(String[] args) {
        System.out.println("--- Démarrage du banc d'essai ---");

        List<TestConfig> testConfigs = generateTestConfigs();
        List<File> images = findImageFiles(new File("Images"));

        if (images.isEmpty()) {
            System.out.println("Aucune image trouvée dans le dossier 'Images'.");
            return;
        }

        // Création des threads
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("Utilisation de " + numThreads + " threads.");

        List<Future<String[]>> futures = new ArrayList<>();

        // chaque image et chaque configuration prend un thread
        for (File imageFile : images) {
            for (TestConfig config : testConfigs) {
                Callable<String[]> task = new TestTask(imageFile, config);
                futures.add(executor.submit(task));
            }
        }

        // Récupération des résultats et écriture dans le CSV pour l'étudier apres execution
        try (PrintWriter writer = new PrintWriter(new FileWriter("resultats_recherche.csv"))) {
            writer.println("Image;AlgoPalette;AlgoEcosysteme;FlouRayon;KMeans_K;DBSCAN_Eps;DBSCAN_MinPts;HAC_K;TempsTotal_ms");

            for (Future<String[]> future : futures) {
                try {
                    String[] result = future.get();
                    writer.println(String.join(";", result));
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Erreur lors de l'exécution d'une tâche: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
        }

        executor.shutdown();
        System.out.println("--- Banc d'essai terminé. Résultats sauvegardés dans 'resultats_recherche.csv' ---");
    }

    /**
     * Génère toutes les combinaisons de configurations de test possible.
     */
    private static List<TestConfig> generateTestConfigs() {
        List<TestConfig> configs = new ArrayList<>();

        for (int flouRayon : FLOU_RAYONS) {
            for (int k : KMEANS_K_VALUES) {
                AlgoExtractionPalette paletteAlgo = new PaletteKmeans(k);

                // Combinaisons avec DBSCAN
                for (double eps : DBSCAN_EPS_VALUES) {
                    for (int minPts : DBSCAN_MIN_PTS_VALUES) {
                        AlgoClustering ecosystemAlgo = new DBSCAN(eps, minPts);
                        configs.add(new TestConfig(paletteAlgo, ecosystemAlgo, flouRayon));
                    }
                }

                // Combinaisons avec HAC
                for (int hacK : HAC_K_VALUES) {
                    AlgoClustering ecosystemAlgo = new HAC(hacK);
                    configs.add(new TestConfig(paletteAlgo, ecosystemAlgo, flouRayon));
                }
            }
        }
        return configs;
    }

    /**
     * Trouve tous les fichiers image dans un dossier.
     */
    private static List<File> findImageFiles(File dir) {
        List<File> imageFiles = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    imageFiles.addAll(findImageFiles(file));
                } else {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".png")) {
                        imageFiles.add(file);
                    }
                }
            }
        }
        return imageFiles;
    }


    /**
     * Classe interne représentant la configuration d'un test.
     */
    private static class TestConfig {
        final AlgoExtractionPalette paletteAlgo;
        final AlgoClustering ecosystemAlgo;
        final int flouRayon;

        TestConfig(AlgoExtractionPalette paletteAlgo, AlgoClustering ecosystemAlgo, int flouRayon) {
            this.paletteAlgo = paletteAlgo;
            this.ecosystemAlgo = ecosystemAlgo;
            this.flouRayon = flouRayon;
        }

        String getKMeansK() {
            return (paletteAlgo instanceof PaletteKmeans) ? String.valueOf(((PaletteKmeans) paletteAlgo).getK()) : "N/A";
        }

        String getDbscanEps() {
            return (ecosystemAlgo instanceof DBSCAN) ? String.valueOf(((DBSCAN) ecosystemAlgo).getEps()) : "N/A";
        }

        String getDbscanMinPts() {
            return (ecosystemAlgo instanceof DBSCAN) ? String.valueOf(((DBSCAN) ecosystemAlgo).getMinPts()) : "N/A";
        }
        
        String getHacK() {
            return (ecosystemAlgo instanceof HAC) ? String.valueOf(((HAC) ecosystemAlgo).getK()) : "N/A";
        }
    }

    /**
     * Classe interne représentant une tâche de test unique.
     */
    private static class TestTask implements Callable<String[]> {
        private final File imageFile;
        private final TestConfig config;

        public TestTask(File imageFile, TestConfig config) {
            this.imageFile = imageFile;
            this.config = config;
        }

        @Override
        public String[] call() throws Exception {
            long startTime = System.currentTimeMillis();

            //Chargement et flou
            BufferedImage originalImage = ImageIO.read(imageFile);
            Flou flou = new FlouMoyenne(config.flouRayon);
            BufferedImage blurredImg = flou.appliquerFlou(imageFile);

            //Génération de la palette
            NormeCouleurs norme = new NormeBetterCIELAB();
            Color[] paletteCouleurs = config.paletteAlgo.extrairePalette(blurredImg, 0, norme);
            Palette paletteBiomes = new Palette(new BiomeMapper(norme).getBiomeMapping(paletteCouleurs), norme);

            //Création de la carte des biomes et regroupement
            Map<Color, List<Point>> pixelsParBiome = new HashMap<>();
            for (Color biomeColor : paletteBiomes.getBiomeColors().values()) {
                pixelsParBiome.put(biomeColor, new ArrayList<>());
            }
            for (int y = 0; y < originalImage.getHeight(); y++) {
                for (int x = 0; x < originalImage.getWidth(); x++) {
                    Color pixelColor = new Color(blurredImg.getRGB(x, y));
                    String biomeName = paletteBiomes.getBiomePlusProche(pixelColor);
                    Color biomeColor = paletteBiomes.getBiomeColors().get(biomeName);
                    pixelsParBiome.get(biomeColor).add(new Point(x, y));
                }
            }

            //Détection des écosystèmes
            for (Map.Entry<Color, List<Point>> entry : pixelsParBiome.entrySet()) {
                List<Point> biomePixels = entry.getValue();

                if (biomePixels.size() > PIXEL_THRESHOLD_FOR_ECOSYSTEM_CLUSTERING) {
                    continue;
                }
                
                if (biomePixels.size() < 2) continue;

                double[][] points = new double[biomePixels.size()][2];
                for (int i = 0; i < biomePixels.size(); i++) {
                    points[i][0] = biomePixels.get(i).getX();
                    points[i][1] = biomePixels.get(i).getY();
                }
                config.ecosystemAlgo.cluster(points);
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.printf("Terminé: %s, %s, %s, Flou=%d en %d ms%n",
                    imageFile.getName(), config.paletteAlgo, config.ecosystemAlgo, config.flouRayon, duration);

            // Écriture du CSV
            return new String[]{
                    imageFile.getName(),
                    config.paletteAlgo.toString(),
                    config.ecosystemAlgo.toString(),
                    String.valueOf(config.flouRayon),
                    config.getKMeansK(),
                    config.getDbscanEps(),
                    config.getDbscanMinPts(),
                    config.getHacK(),
                    String.valueOf(duration)
            };
        }
    }
}

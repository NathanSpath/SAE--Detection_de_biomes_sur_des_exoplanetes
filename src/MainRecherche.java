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
import java.util.logging.*;

public class MainRecherche {

    private static final Logger logger = Logger.getLogger(MainRecherche.class.getName());

    // --- CONFIGURATION DES TESTS ---
    private static final int[] FLOU_RAYONS = {3, 5};
    private static final int[] KMEANS_K_VALUES = {10, 15, 20};
    private static final double[] DBSCAN_EPS_VALUES = {3.0, 5.0};
    private static final int[] DBSCAN_MIN_PTS_VALUES = {20, 50};
    private static final int[] HAC_K_VALUES = {10, 20};
    private static final int PIXEL_THRESHOLD_FOR_ECOSYSTEM_CLUSTERING = 250_000;

    public static void main(String[] args) {
        try {
            setupLogger();
            logger.info("--- Démarrage du banc d'essai ---");

            List<TestConfig> testConfigs = generateTestConfigs();
            List<File> images = findImageFiles(new File("Images"));

            if (images.isEmpty()) {
                logger.warning("Aucune image trouvée dans le dossier 'Images'.");
                return;
            }

            int numThreads = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            logger.info("Utilisation de " + numThreads + " threads.");

            List<Future<String[]>> futures = new ArrayList<>();

            for (File imageFile : images) {
                for (TestConfig config : testConfigs) {
                    Callable<String[]> task = new TestTask(imageFile, config);
                    futures.add(executor.submit(task));
                }
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter("resultats_recherche.csv"))) {
                writer.println("Image;AlgoPalette;AlgoEcosysteme;FlouRayon;KMeans_K;DBSCAN_Eps;DBSCAN_MinPts;HAC_K;TempsTotal_ms");
                for (Future<String[]> future : futures) {
                    try {
                        String[] result = future.get();
                        writer.println(String.join(";", result));
                    } catch (InterruptedException | ExecutionException e) {
                        logger.log(Level.SEVERE, "Erreur lors de l'exécution d'une tâche", e);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Erreur lors de l'écriture du fichier CSV", e);
            }

            executor.shutdown();
            logger.info("--- Banc d'essai terminé. Résultats sauvegardés dans 'resultats_recherche.csv' ---");
        } catch (IOException e) {
            System.err.println("Impossible de configurer le logger: " + e.getMessage());
        }
    }

    private static void setupLogger() throws IOException {
        logger.setUseParentHandlers(false);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);

        FileHandler fileHandler = new FileHandler("main_recherche.log");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        logger.setLevel(Level.INFO);
    }

    private static List<TestConfig> generateTestConfigs() {
        List<TestConfig> configs = new ArrayList<>();
        for (int flouRayon : FLOU_RAYONS) {
            for (int k : KMEANS_K_VALUES) {
                AlgoExtractionPalette paletteAlgo = new PaletteKmeans(k);
                for (double eps : DBSCAN_EPS_VALUES) {
                    for (int minPts : DBSCAN_MIN_PTS_VALUES) {
                        configs.add(new TestConfig(paletteAlgo, new DBSCAN(eps, minPts), flouRayon));
                    }
                }
                for (int hacK : HAC_K_VALUES) {
                    configs.add(new TestConfig(paletteAlgo, new HAC(hacK), flouRayon));
                }
            }
        }
        logger.info("Génération de " + configs.size() + " configurations de test.");
        return configs;
    }

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

    private static class TestConfig {
        final AlgoExtractionPalette paletteAlgo;
        final AlgoClustering ecosystemAlgo;
        final int flouRayon;

        TestConfig(AlgoExtractionPalette paletteAlgo, AlgoClustering ecosystemAlgo, int flouRayon) {
            this.paletteAlgo = paletteAlgo;
            this.ecosystemAlgo = ecosystemAlgo;
            this.flouRayon = flouRayon;
        }

        String getKMeansK() { return (paletteAlgo instanceof PaletteKmeans) ? String.valueOf(((PaletteKmeans) paletteAlgo).getK()) : "N/A"; }
        String getDbscanEps() { return (ecosystemAlgo instanceof DBSCAN) ? String.valueOf(((DBSCAN) ecosystemAlgo).getEps()) : "N/A"; }
        String getDbscanMinPts() { return (ecosystemAlgo instanceof DBSCAN) ? String.valueOf(((DBSCAN) ecosystemAlgo).getMinPts()) : "N/A"; }
        String getHacK() { return (ecosystemAlgo instanceof HAC) ? String.valueOf(((HAC) ecosystemAlgo).getK()) : "N/A"; }
    }

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
            String threadName = Thread.currentThread().getName();
            logger.info(String.format("[%s] Début: %s, %s, %s, Flou=%d", threadName, imageFile.getName(), config.paletteAlgo, config.ecosystemAlgo, config.flouRayon));

            BufferedImage originalImage = ImageIO.read(imageFile);
            Flou flou = new FlouMoyenne(config.flouRayon);
            BufferedImage blurredImg = flou.appliquerFlou(imageFile);

            NormeCouleurs norme = new NormeBetterCIELAB();
            Color[] paletteCouleurs = config.paletteAlgo.extrairePalette(blurredImg, 0, norme);
            Palette paletteBiomes = new Palette(new BiomeMapper(norme).getBiomeMapping(paletteCouleurs), norme);

            Map<Color, List<Point>> pixelsParBiome = new HashMap<>();
            paletteBiomes.getBiomeColors().values().forEach(color -> pixelsParBiome.put(color, new ArrayList<>()));
            for (int y = 0; y < originalImage.getHeight(); y++) {
                for (int x = 0; x < originalImage.getWidth(); x++) {
                    String biomeName = paletteBiomes.getBiomePlusProche(new Color(blurredImg.getRGB(x, y)));
                    pixelsParBiome.get(paletteBiomes.getBiomeColors().get(biomeName)).add(new Point(x, y));
                }
            }

            for (Map.Entry<Color, List<Point>> entry : pixelsParBiome.entrySet()) {
                List<Point> biomePixels = entry.getValue();
                if (biomePixels.size() > PIXEL_THRESHOLD_FOR_ECOSYSTEM_CLUSTERING || biomePixels.size() < 2) continue;
                double[][] points = biomePixels.stream().map(p -> new double[]{p.getX(), p.getY()}).toArray(double[][]::new);
                config.ecosystemAlgo.cluster(points);
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info(String.format("[%s] Terminé: %s en %d ms", threadName, imageFile.getName(), duration));

            return new String[]{
                imageFile.getName(), config.paletteAlgo.toString(), config.ecosystemAlgo.toString(),
                String.valueOf(config.flouRayon), config.getKMeansK(), config.getDbscanEps(),
                config.getDbscanMinPts(), config.getHacK(), String.valueOf(duration)
            };
        }
    }
}
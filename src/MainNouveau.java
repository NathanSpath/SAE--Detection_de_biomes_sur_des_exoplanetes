import AlgoCluster.DBSCAN;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.*;

public class MainNouveau {

    private static final Logger logger = Logger.getLogger(MainNouveau.class.getName());

    // --- PARAMÈTRES OPTIMISÉS ET ÉQUILIBRÉS ---
    static final int NB_THREADS = Runtime.getRuntime().availableProcessors();
    static final String IMAGE_INPUT_PATH = "Images/Planete_1.jpg";
    static final String OUTPUT_DIR = "rendus_individuels";

    // Paramètres de flou
    static final int FLOU_RAYON = 3;

    // Paramètres de la palette (K-Means)
    static final int KMEANS_K = 20; // Un k plus élevé crée des biomes plus petits, accélérant DBSCAN.
    static final double SEUIL_SIMILARITE = 15.0;

    // Paramètres des écosystèmes (DBSCAN)
    static final double DBSCAN_EPS = 3.0;      // eps=3 est bien plus rapide que 5.
    static final int DBSCAN_MIN_PTS = 10;     // minPts=10 est un bon compromis pour trouver des clusters sans être trop lent.


    public static void main(String[] args) {
        try {
            setupLogger();
            logger.info("--- Démarrage du pipeline avec paramètres optimisés ---");
            logger.info("Utilisation de " + NB_THREADS + " threads.");

            runPipeline();

            logger.info("\n--- Pipeline de visualisation terminé. Résultats dans le dossier '" + OUTPUT_DIR + "' ---");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Une erreur critique est survenue dans le pipeline.", e);
        }
    }

    private static void setupLogger() throws IOException {
        logger.setUseParentHandlers(false);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);

        FileHandler fileHandler = new FileHandler("main_nouveau.log");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        logger.setLevel(Level.INFO);
    }

    private static void runPipeline() throws IOException, InterruptedException, ExecutionException {
        // --- 1. ÉTAPES GLOBALES (THREAD PRINCIPAL) ---
        File inputFile = new File(IMAGE_INPUT_PATH);
        if (!inputFile.exists()) {
            logger.severe("Erreur : Fichier d'entrée introuvable.");
            return;
        }
        BufferedImage originalImage = ImageIO.read(inputFile);
        String imageName = inputFile.getName().split("\\.")[0];

        new File(OUTPUT_DIR).mkdirs();
        new File(OUTPUT_DIR + "/" + imageName + "_biomes").mkdirs();
        new File(OUTPUT_DIR + "/" + imageName + "_ecosystemes_par_biome").mkdirs();

        Flou flou = new FlouMoyenne(FLOU_RAYON);
        BufferedImage blurredImg = flou.appliquerFlou(inputFile);
        logger.info("Image pré-traitée avec un filtre de flou (rayon=" + FLOU_RAYON + ").");

        NormeCouleurs norme = new NormeBetterCIELAB();
        AlgoExtractionPalette kmeans = new PaletteKmeans(KMEANS_K);
        Color[] paletteCouleurs = kmeans.extrairePalette(blurredImg, KMEANS_K, norme);
        Palette paletteBiomes = new Palette(new BiomeMapper(norme).getBiomeMapping(paletteCouleurs), norme);
        logger.info("Palette de biomes globale créée (k=" + KMEANS_K + ").");

        // --- 2. PARALLÉLISATION DE LA CARTOGRAPHIE DES BIOMES ---
        logger.info("Cartographie des biomes en parallèle...");
        ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
        Map<String, List<Point>> pixelsParBiome = mapBiomesParallel(blurredImg, paletteBiomes, executor);
        logger.info("Cartographie des biomes terminée.");

        // --- 3. PARALLÉLISATION DE L'ANALYSE ET DE LA VISUALISATION ---
        logger.info("\nGénération des images de biomes et écosystèmes en parallèle...");
        BufferedImage lightBackground = createLightBackground(originalImage, 0.75f);
        List<Callable<Void>> visualizationTasks = new ArrayList<>();

        for (Map.Entry<String, List<Point>> entry : pixelsParBiome.entrySet()) {
            String biomeName = entry.getKey();
            List<Point> biomePixels = entry.getValue();

            Callable<Void> task = () -> {
                visualizeBiome(imageName, biomeName, biomePixels, lightBackground, originalImage);
                visualizeEcosystemsForBiome(imageName, biomeName, biomePixels, lightBackground);
                return null;
            };
            visualizationTasks.add(task);
        }

        executor.invokeAll(visualizationTasks);
        executor.shutdown();
    }

    private static Map<String, List<Point>> mapBiomesParallel(BufferedImage blurredImg, Palette palette, ExecutorService executor) throws InterruptedException, ExecutionException {
        List<Future<Map<String, List<Point>>>> futures = new ArrayList<>();
        int height = blurredImg.getHeight();
        int stripHeight = (height + NB_THREADS - 1) / NB_THREADS;

        for (int i = 0; i < NB_THREADS; i++) {
            int startY = i * stripHeight;
            int endY = Math.min(startY + stripHeight, height);
            Callable<Map<String, List<Point>>> task = () -> {
                Map<String, List<Point>> partialMap = new HashMap<>();
                for (String biomeName : palette.getBiomeColors().keySet()) {
                    partialMap.put(biomeName, new ArrayList<>());
                }
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < blurredImg.getWidth(); x++) {
                        Color pixelColor = new Color(blurredImg.getRGB(x, y));
                        String biomeName = palette.getBiomePlusProche(pixelColor);
                        if (biomeName != null) {
                            partialMap.get(biomeName).add(new Point(x, y));
                        }
                    }
                }
                return partialMap;
            };
            futures.add(executor.submit(task));
        }

        Map<String, List<Point>> finalMap = new HashMap<>();
        for (Future<Map<String, List<Point>>> future : futures) {
            future.get().forEach((biomeName, points) -> finalMap.merge(biomeName, points, (l1, l2) -> {
                l1.addAll(l2);
                return l1;
            }));
        }
        return finalMap;
    }

    private static void visualizeBiome(String imageName, String biomeName, List<Point> biomePixels, BufferedImage lightBackground, BufferedImage originalImage) throws IOException {
        if (biomePixels.isEmpty()) return;

        BufferedImage biomeImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = biomeImage.createGraphics();
        g2d.drawImage(lightBackground, 0, 0, null);
        for (Point p : biomePixels) {
            biomeImage.setRGB(p.x, p.y, originalImage.getRGB(p.x, p.y));
        }
        g2d.dispose();
        String biomeFileName = String.format("%s/%s_biomes/%s_biome_%s.png", OUTPUT_DIR, imageName, imageName, biomeName);
        ImageIO.write(biomeImage, "PNG", new File(biomeFileName));
        logger.info(" -> Image de biome sauvegardée : " + biomeFileName);
    }

    private static void visualizeEcosystemsForBiome(String imageName, String biomeName, List<Point> biomePixels, BufferedImage lightBackground) throws IOException {
        if (biomePixels.size() < DBSCAN_MIN_PTS) {
            logger.info(" -> Biome '" + biomeName + "' ignoré pour les écosystèmes (pas assez de pixels).");
            return;
        }

        logger.info("Traitement des écosystèmes pour le biome : " + biomeName);
        DBSCAN dbscan = new DBSCAN(DBSCAN_EPS, DBSCAN_MIN_PTS);
        double[][] points = new double[biomePixels.size()][2];
        for (int i = 0; i < biomePixels.size(); i++) {
            points[i][0] = biomePixels.get(i).getX();
            points[i][1] = biomePixels.get(i).getY();
        }
        int[] labels = dbscan.cluster(points);

        Map<Integer, List<Point>> pixelsParEcosysteme = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            int clusterId = labels[i];
            if (clusterId == 0) continue;
            pixelsParEcosysteme.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(biomePixels.get(i));
        }
        
        if (pixelsParEcosysteme.isEmpty()) {
            logger.warning(" -> Aucun écosystème trouvé pour le biome '" + biomeName + "' avec les paramètres actuels.");
            return;
        }

        BufferedImage ecosystemImage = new BufferedImage(lightBackground.getWidth(), lightBackground.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = ecosystemImage.createGraphics();
        g2d.drawImage(lightBackground, 0, 0, null);

        Random rand = new Random();
        for (List<Point> ecosystemPixels : pixelsParEcosysteme.values()) {
            Color flashyColor = new Color(rand.nextInt(200) + 55, rand.nextInt(200) + 55, rand.nextInt(200) + 55);
            for (Point p : ecosystemPixels) {
                ecosystemImage.setRGB(p.x, p.y, flashyColor.getRGB());
            }
        }
        g2d.dispose();

        String ecosystemFileName = String.format("%s/%s_ecosystemes_par_biome/%s_ecosystemes_%s.png", OUTPUT_DIR, imageName, imageName, biomeName);
        ImageIO.write(ecosystemImage, "PNG", new File(ecosystemFileName));
        logger.info(" -> Carte des écosystèmes pour '" + biomeName + "' sauvegardée. (" + pixelsParEcosysteme.size() + " écosystèmes)");
    }

    private static BufferedImage createLightBackground(BufferedImage original, float percentage) {
        BufferedImage lightImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color c = new Color(original.getRGB(x, y));
                int newR = Math.round(c.getRed() + percentage * (255 - c.getRed()));
                int newG = Math.round(c.getGreen() + percentage * (255 - c.getGreen()));
                int newB = Math.round(c.getBlue() + percentage * (255 - c.getBlue()));
                lightImage.setRGB(x, y, new Color(newR, newG, newB).getRGB());
            }
        }
        return lightImage;
    }
}
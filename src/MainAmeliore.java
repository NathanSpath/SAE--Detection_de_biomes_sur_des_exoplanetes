import AlgoCluster.DBSCAN;
import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import flou.Flou;
import flou.FlouGausien;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.*;

public class MainAmeliore {

    private static final Logger logger = Logger.getLogger(MainAmeliore.class.getName());

    public static class Config {
        public static final int NB_THREADS = Runtime.getRuntime().availableProcessors();
        public static final String IMAGE_INPUT_PATH = "Images/Planete_5.jpeg";
        public static final String OUTPUT_DIR = "resultats_ameliores";

        public static final int KMEANS_NB_COULEURS = 40;
        public static final double SEUIL_SIMILARITE_COULEUR = 18.0;

        public static final int FLOU_GAUSSIEN_KERNEL_SIZE = 7;
        public static final double FLOU_GAUSSIEN_SIGMA = 3.0;

        public static final int SEUIL_MODE_RAPIDE = 75_000;
        public static final int SEUIL_MODE_TRES_RAPIDE = 500_000;

        public static final double DBSCAN_EPS_NORMAL = 3.0;
        public static final int DBSCAN_MIN_PTS_NORMAL = 20;

        public static final int GRILLE_MODE_RAPIDE_TAILLE = 800;
        public static final double DBSCAN_EPS_RAPIDE = 4.0;
        public static final int DBSCAN_MIN_PTS_RAPIDE = 5;

        public static final int MAX_PIXELS_TRES_RAPIDE = 150_000;
        public static final int GRILLE_MODE_TRES_RAPIDE_TAILLE = 600;
        public static final double DBSCAN_EPS_TRES_RAPIDE = 4.0;
        public static final int DBSCAN_MIN_PTS_TRES_RAPIDE = 5;
    }

    public static void main(String[] args) {
        try {
            setupLogger();
            logger.info("--- Démarrage du pipeline AMÉLIORÉ pour la QUALITÉ ---");
            long startTime = System.currentTimeMillis();
            runPipeline();
            long endTime = System.currentTimeMillis();
            logger.info("\n--- Pipeline AMÉLIORÉ terminé en " + (endTime - startTime) / 1000.0 + " secondes. ---");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur critique.", e);
        }
    }

    private static void setupLogger() throws IOException {
        logger.setUseParentHandlers(false);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);
        FileHandler fileHandler = new FileHandler("analyse_ameliore.log");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.setLevel(Level.INFO);
    }

    private static void runPipeline() throws IOException, InterruptedException, ExecutionException {
        File inputFile = new File(Config.IMAGE_INPUT_PATH);
        BufferedImage originalImage = ImageIO.read(inputFile);
        String imageName = inputFile.getName().split("\\.")[0];

        File mainOutputDir = new File(Config.OUTPUT_DIR);
        File biomesDir = new File(mainOutputDir, imageName + "_biomes");
        File ecosystemsDir = new File(mainOutputDir, imageName + "_ecosystemes");
        mainOutputDir.mkdirs(); biomesDir.mkdirs(); ecosystemsDir.mkdirs();

        Flou flou = new FlouGausien(Config.FLOU_GAUSSIEN_KERNEL_SIZE, Config.FLOU_GAUSSIEN_KERNEL_SIZE, Config.FLOU_GAUSSIEN_SIGMA);
        BufferedImage blurredImg = flou.appliquerFlou(inputFile);
        ImageIO.write(blurredImg, "PNG", new File(mainOutputDir, imageName + "_flou.png"));
        logger.info("Étape 1: Image pré-traitée et sauvegardée.");

        NormeCouleurs norme = new NormeBetterCIELAB();
        AlgoExtractionPalette kmeans = new PaletteKmeans(Config.KMEANS_NB_COULEURS);
        Color[] couleursCandidates = kmeans.extrairePalette(blurredImg, Config.KMEANS_NB_COULEURS, norme);
        Color[] couleursFiltrees = filtrerCouleursUniques(couleursCandidates, norme, Config.SEUIL_SIMILARITE_COULEUR);
        Palette paletteBiomes = new Palette(new BiomeMapper(norme).getBiomeMapping(couleursFiltrees), norme);
        generatePaletteImage(paletteBiomes, mainOutputDir.getPath(), imageName);
        logger.info("Étape 2: Palette de " + paletteBiomes.getNbBiomes() + " biomes créée et sauvegardée.");

        ExecutorService executor = Executors.newFixedThreadPool(Config.NB_THREADS);
        Map<String, List<Point>> pixelsParBiome = mapBiomesParallel(blurredImg, paletteBiomes, executor);
        logger.info("Étape 3: Cartographie des biomes terminée.");

        logger.info("\nÉtape 4: Génération des images...");
        BufferedImage lightBackground = createLightBackground(originalImage, 0.75f);
        List<Callable<Void>> visualizationTasks = new ArrayList<>();
        for (Map.Entry<String, List<Point>> entry : pixelsParBiome.entrySet()) {
            String biomeName = entry.getKey();
            List<Point> biomePixels = entry.getValue();
            visualizationTasks.add(() -> {
                visualizeBiome(imageName, biomeName, biomePixels, lightBackground, originalImage, biomesDir.getPath(), paletteBiomes);
                visualizeEcosystemsForBiome(imageName, biomeName, biomePixels, lightBackground, ecosystemsDir.getPath());
                return null;
            });
        }
        executor.invokeAll(visualizationTasks);
        executor.shutdown();
    }

    private static void generatePaletteImage(Palette palette, String outputDir, String imageName) throws IOException {
        if (palette.getNbBiomes() == 0) {
            logger.warning("Aucune couleur dans la palette, image non générée.");
            return;
        }
        int width = 800;
        int height = 100;
        BufferedImage imgPalette = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgPalette.createGraphics();
        int colorWidth = width / palette.getNbBiomes();
        int currentX = 0;
        for (Map.Entry<String, Color> entry : palette.getBiomeColors().entrySet()) {
            g2d.setColor(entry.getValue());
            g2d.fillRect(currentX, 0, colorWidth, height);
            g2d.setColor(Color.BLACK);
            g2d.drawString(entry.getKey(), currentX + 5, 20);
            currentX += colorWidth;
        }
        g2d.dispose();
        String paletteFileName = String.format("%s/%s_palette.png", outputDir, imageName);
        ImageIO.write(imgPalette, "PNG", new File(paletteFileName));
    }

    private static void visualizeEcosystemsForBiome(String imageName, String biomeName, List<Point> biomePixels, BufferedImage lightBackground, String outputDir) throws IOException {
        if (biomePixels.size() < Config.DBSCAN_MIN_PTS_NORMAL) return;

        Map<Integer, List<Point>> pixelsParEcosysteme;
        if (biomePixels.size() > Config.SEUIL_MODE_TRES_RAPIDE) {
            logger.info("   -> [TRES RAPIDE] '" + biomeName + "' (" + biomePixels.size() + " pixels)...");
            pixelsParEcosysteme = clusterSubsampledGridMode(biomePixels, lightBackground.getWidth(), lightBackground.getHeight());
        } else if (biomePixels.size() > Config.SEUIL_MODE_RAPIDE) {
            logger.info("   -> [RAPIDE] '" + biomeName + "' (" + biomePixels.size() + " pixels)...");
            pixelsParEcosysteme = clusterGridMode(biomePixels, lightBackground.getWidth(), lightBackground.getHeight(), Config.GRILLE_MODE_RAPIDE_TAILLE, Config.DBSCAN_EPS_RAPIDE, Config.DBSCAN_MIN_PTS_RAPIDE);
        } else {
            logger.info("   -> [NORMAL] '" + biomeName + "' (" + biomePixels.size() + " pixels)...");
            pixelsParEcosysteme = clusterNormalMode(biomePixels);
        }

        if (pixelsParEcosysteme == null || pixelsParEcosysteme.isEmpty()) {
            logger.warning("Aucun écosystème pour '" + biomeName + "'.");
            return;
        }

        BufferedImage ecosystemImage = new BufferedImage(lightBackground.getWidth(), lightBackground.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = ecosystemImage.createGraphics();
        g2d.drawImage(lightBackground, 0, 0, null);
        Random rand = new Random(42);
        for (List<Point> ecosystemPixels : pixelsParEcosysteme.values()) {
            Color flashyColor = new Color(rand.nextInt(200) + 55, rand.nextInt(200) + 55, rand.nextInt(200) + 55);
            for (Point p : ecosystemPixels) ecosystemImage.setRGB(p.x, p.y, flashyColor.getRGB());
        }
        g2d.dispose();
        ImageIO.write(ecosystemImage, "PNG", new File(String.format("%s/%s_ecosystemes_%s.png", outputDir, imageName, biomeName.replaceAll("\\s+", "_"))));
    }

    private static Map<Integer, List<Point>> clusterNormalMode(List<Point> biomePixels) {
        DBSCAN dbscan = new DBSCAN(Config.DBSCAN_EPS_NORMAL, Config.DBSCAN_MIN_PTS_NORMAL);
        double[][] points = biomePixels.stream().map(p -> new double[]{p.getX(), p.getY()}).toArray(double[][]::new);
        int[] labels = dbscan.cluster(points);
        Map<Integer, List<Point>> result = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] != 0) result.computeIfAbsent(labels[i], k -> new ArrayList<>()).add(biomePixels.get(i));
        }
        return result;
    }

    private static Map<Integer, List<Point>> clusterGridMode(List<Point> biomePixels, int imgWidth, int imgHeight, int gridSize, double eps, int minPts) {
        int minX = imgWidth, maxX = 0, minY = imgHeight, maxY = 0;
        for (Point p : biomePixels) {
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y;
        }
        double scale = (double) gridSize / Math.max(maxX - minX + 1, maxY - minY + 1);
        int gridW = Math.max(1, (int) Math.ceil((maxX - minX + 1) * scale));
        int gridH = Math.max(1, (int) Math.ceil((maxY - minY + 1) * scale));

        @SuppressWarnings("unchecked")
        List<Point>[] cellPixels = new List[gridW * gridH];
        for (Point p : biomePixels) {
            int cx = Math.min((int) ((p.x - minX) * scale), gridW - 1);
            int cy = Math.min((int) ((p.y - minY) * scale), gridH - 1);
            int idx = cy * gridW + cx;
            if (cellPixels[idx] == null) cellPixels[idx] = new ArrayList<>();
            cellPixels[idx].add(p);
        }

        List<double[]> gridPoints = new ArrayList<>();
        List<Integer> gridIndices = new ArrayList<>();
        for (int i = 0; i < cellPixels.length; i++) {
            if (cellPixels[i] != null) {
                gridPoints.add(new double[]{i % gridW, (double) i / gridW});
                gridIndices.add(i);
            }
        }
        if (gridPoints.isEmpty()) return new HashMap<>();

        DBSCAN dbscan = new DBSCAN(eps, minPts);
        int[] labels = dbscan.cluster(gridPoints.toArray(new double[0][]));
        Map<Integer, List<Point>> result = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] != 0) result.computeIfAbsent(labels[i], k -> new ArrayList<>()).addAll(cellPixels[gridIndices.get(i)]);
        }
        return result;
    }

    private static Map<Integer, List<Point>> clusterSubsampledGridMode(List<Point> biomePixels, int imgWidth, int imgHeight) {
        List<Point> sampledPixels = new ArrayList<>(biomePixels);
        Collections.shuffle(sampledPixels, new Random(42));
        sampledPixels = sampledPixels.subList(0, Math.min(sampledPixels.size(), Config.MAX_PIXELS_TRES_RAPIDE));
        return clusterGridMode(sampledPixels, imgWidth, imgHeight, Config.GRILLE_MODE_TRES_RAPIDE_TAILLE, Config.DBSCAN_EPS_TRES_RAPIDE, Config.DBSCAN_MIN_PTS_TRES_RAPIDE);
    }

    private static Color[] filtrerCouleursUniques(Color[] couleurs, NormeCouleurs norme, double seuil) {
        List<Color> uniques = new ArrayList<>();
        for (Color c : couleurs) {
            if (uniques.stream().noneMatch(u -> norme.distanceCouleur(c, u) < seuil)) uniques.add(c);
        }
        return uniques.toArray(new Color[0]);
    }

    private static void visualizeBiome(String imageName, String biomeName, List<Point> biomePixels, BufferedImage lightBackground, BufferedImage originalImage, String outputDir, Palette palette) throws IOException {
        if (biomePixels.isEmpty()) return;

        Color biomeColor = palette.getBiomeColor(biomeName);
        if (biomeColor == null) {
            logger.warning("Couleur non trouvée pour le biome: " + biomeName + ". Utilisation du noir.");
            biomeColor = Color.BLACK;
        }

        BufferedImage biomeImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = biomeImage.createGraphics();
        g2d.drawImage(lightBackground, 0, 0, null);

        g2d.setColor(biomeColor);
        for (Point p : biomePixels) {
            biomeImage.setRGB(p.x, p.y, biomeColor.getRGB());
        }
        g2d.dispose();

        String fileName = String.format("%s/%s_biome_%s.png", outputDir, imageName, biomeName.replaceAll("\\s+", "_"));
        ImageIO.write(biomeImage, "PNG", new File(fileName));
    }
    
    private static Map<String, List<Point>> mapBiomesParallel(BufferedImage img, Palette palette, ExecutorService executor) throws InterruptedException, ExecutionException {
        List<Future<Map<String, List<Point>>>> futures = new ArrayList<>();
        int stripHeight = (img.getHeight() + Config.NB_THREADS - 1) / Config.NB_THREADS;
        for (int i = 0; i < Config.NB_THREADS; i++) {
            final int startY = i * stripHeight;
            final int endY = Math.min(startY + stripHeight, img.getHeight());
            futures.add(executor.submit(() -> {
                Map<String, List<Point>> partialMap = new HashMap<>();
                palette.getBiomeColors().keySet().forEach(name -> partialMap.put(name, new ArrayList<>()));
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        partialMap.get(palette.getBiomePlusProche(new Color(img.getRGB(x, y)))).add(new Point(x, y));
                    }
                }
                return partialMap;
            }));
        }
        Map<String, List<Point>> finalMap = new ConcurrentHashMap<>();
        for (Future<Map<String, List<Point>>> future : futures) {
            future.get().forEach((k, v) -> finalMap.merge(k, v, (l1, l2) -> { l1.addAll(l2); return l1; }));
        }
        return finalMap;
    }

    private static BufferedImage createLightBackground(BufferedImage original, float percentage) {
        BufferedImage lightImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color c = new Color(original.getRGB(x, y));
                int r = (int) (c.getRed() + (255 - c.getRed()) * percentage);
                int g = (int) (c.getGreen() + (255 - c.getGreen()) * percentage);
                int b = (int) (c.getBlue() + (255 - c.getBlue()) * percentage);
                lightImage.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return lightImage;
    }
}

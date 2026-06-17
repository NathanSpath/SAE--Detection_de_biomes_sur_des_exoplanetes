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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.*;

public class Main {

    // --- LOGGER ---
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    // --- PARAMÈTRES GLOBAUX ---
    private static final int NB_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int NB_COULEURS_PALETTE = 20;
    private static final double SEUIL_SIMILARITE_COULEUR = 15.0;
    private static final String IMAGE_INPUT_PATH = "Images/Planete_1.jpg";
    private static final String OUTPUT_DIR = "resultats_analyse";

    /**
     * Point d'entrée principal du programme.
     */
    public static void main(String[] args) {
        try {
            setupLogger();
            logger.info("--- Démarrage du pipeline de détection de biomes et écosystèmes ---");
            logger.info("Paramètres : ");
            logger.info("  - Threads utilisés : " + NB_THREADS);
            logger.info("  - Couleurs K-Means : " + NB_COULEURS_PALETTE);
            logger.info("  - Seuil similarité : " + SEUIL_SIMILARITE_COULEUR);
            logger.info("  - Image d'entrée : " + IMAGE_INPUT_PATH);
            logger.info("  - Répertoire de sortie : " + OUTPUT_DIR);

            long startTime = System.currentTimeMillis();

            runPipeline();

            long endTime = System.currentTimeMillis();
            logger.info("\n--- Pipeline terminé en " + (endTime - startTime) / 1000.0 + " secondes. ---");
            logger.info("Résultats disponibles dans le répertoire : '" + OUTPUT_DIR + "'");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Une erreur critique a interrompu le pipeline.", e);
        }
    }

    /**
     * Configure le logger pour écrire dans un fichier et sur la console.
     */
    private static void setupLogger() throws IOException {
        logger.setUseParentHandlers(false); // Désactive la sortie console par défaut
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);

        FileHandler fileHandler = new FileHandler("analyse.log");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        logger.setLevel(Level.INFO);
    }

    /**
     * Exécute le pipeline complet de traitement d'image.
     */
    private static void runPipeline() throws IOException, InterruptedException, ExecutionException {
        // --- 1. INITIALISATION ET PRÉ-TRAITEMENT ---
        File inputFile = new File(IMAGE_INPUT_PATH);
        if (!inputFile.exists()) {
            logger.severe("Erreur : Fichier d'entrée introuvable : " + IMAGE_INPUT_PATH);
            return;
        }
        BufferedImage originalImage = ImageIO.read(inputFile);
        String imageName = inputFile.getName().split("\\.")[0];

        File mainOutputDir = new File(OUTPUT_DIR);
        File biomesDir = new File(mainOutputDir, imageName + "_biomes");
        File ecosystemsDir = new File(mainOutputDir, imageName + "_ecosystemes");
        mainOutputDir.mkdirs();
        biomesDir.mkdirs();
        ecosystemsDir.mkdirs();
        logger.info("Répertoires de sortie créés.");

        Flou flou = new FlouMoyenne(3);
        BufferedImage blurredImg = flou.appliquerFlou(inputFile);
        logger.info("Étape 1: Image pré-traitée avec un filtre de flou.");

        // --- 2. EXTRACTION ET GÉNÉRATION DE LA PALETTE DE BIOMES ---
        NormeCouleurs norme = new NormeBetterCIELAB();
        AlgoExtractionPalette kmeans = new PaletteKmeans(NB_COULEURS_PALETTE);

        Color[] couleursCandidates = kmeans.extrairePalette(blurredImg, NB_COULEURS_PALETTE, norme);
        Color[] couleursFiltrees = filtrerCouleursUniques(couleursCandidates, norme, SEUIL_SIMILARITE_COULEUR);

        BiomeMapper mapper = new BiomeMapper(norme);
        Palette paletteBiomes = new Palette(mapper.getBiomeMapping(couleursFiltrees), norme);
        logger.info("Étape 2: Palette de " + paletteBiomes.getNbBiomes() + " biomes créée et filtrée.");

        generatePaletteImage(paletteBiomes, mainOutputDir.getPath(), imageName);

        // --- 3. CARTOGRAPHIE DES BIOMES (PARALLÉLISÉE) ---
        logger.info("Étape 3: Cartographie des pixels par biome en parallèle...");
        ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
        Map<String, List<Point>> pixelsParBiome = mapBiomesParallel(blurredImg, paletteBiomes, executor);
        logger.info("Cartographie des biomes terminée.");

        // --- 4. VISUALISATION DES BIOMES ET ÉCOSYSTÈMES (PARALLÉLISÉE) ---
        logger.info("\nÉtape 4: Génération des images de biomes et d'écosystèmes en parallèle...");
        BufferedImage lightBackground = createLightBackground(originalImage, 0.75f);
        List<Callable<Void>> visualizationTasks = new ArrayList<>();

        for (Map.Entry<String, List<Point>> entry : pixelsParBiome.entrySet()) {
            String biomeName = entry.getKey();
            List<Point> biomePixels = entry.getValue();

            Callable<Void> task = () -> {
                visualizeBiome(imageName, biomeName, biomePixels, lightBackground, originalImage, biomesDir.getPath());
                visualizeEcosystemsForBiome(imageName, biomeName, biomePixels, lightBackground, ecosystemsDir.getPath());
                return null;
            };
            visualizationTasks.add(task);
        }

        executor.invokeAll(visualizationTasks);
        executor.shutdown();
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

    private static void generatePaletteImage(Palette palette, String outputDir, String imageName) throws IOException {
        if (palette.getNbBiomes() == 0) {
            logger.warning("Aucune couleur dans la palette finale, l'image de la palette n'a pas été générée.");
            return;
        }
        int width = 800, height = 100;
        BufferedImage imgPalette = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgPalette.createGraphics();
        int colorWidth = width / palette.getNbBiomes();
        int currentX = 0;
        for (Map.Entry<String, Color> entry : palette.getBiomeColors().entrySet()) {
            g2d.setColor(entry.getValue());
            g2d.fillRect(currentX, 0, colorWidth, height);
            g2d.setColor(Color.WHITE);
            g2d.drawString(entry.getKey(), currentX + 5, 20);
            currentX += colorWidth;
        }
        g2d.dispose();
        String paletteFileName = String.format("%s/%s_palette.png", outputDir, imageName);
        ImageIO.write(imgPalette, "PNG", new File(paletteFileName));
        logger.info("Image de la palette sauvegardée : " + paletteFileName);
    }

    private static Map<String, List<Point>> mapBiomesParallel(BufferedImage blurredImg, Palette palette, ExecutorService executor) throws InterruptedException, ExecutionException {
        List<Future<Map<String, List<Point>>>> futures = new ArrayList<>();
        int height = blurredImg.getHeight();
        int stripHeight = (height + NB_THREADS - 1) / NB_THREADS;
        for (int i = 0; i < NB_THREADS; i++) {
            final int startY = i * stripHeight;
            final int endY = Math.min(startY + stripHeight, height);
            Callable<Map<String, List<Point>>> task = () -> {
                Map<String, List<Point>> partialMap = new HashMap<>();
                palette.getBiomeColors().keySet().forEach(name -> partialMap.put(name, new ArrayList<>()));
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < blurredImg.getWidth(); x++) {
                        String biomeName = palette.getBiomePlusProche(new Color(blurredImg.getRGB(x, y)));
                        if (biomeName != null) partialMap.get(biomeName).add(new Point(x, y));
                    }
                }
                return partialMap;
            };
            futures.add(executor.submit(task));
        }
        Map<String, List<Point>> finalMap = new ConcurrentHashMap<>();
        for (Future<Map<String, List<Point>>> future : futures) {
            future.get().forEach((biomeName, points) -> finalMap.merge(biomeName, points, (l1, l2) -> { l1.addAll(l2); return l1; }));
        }
        return finalMap;
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

    private static void visualizeBiome(String imageName, String biomeName, List<Point> biomePixels, BufferedImage lightBackground, BufferedImage originalImage, String outputDir) throws IOException {
        if (biomePixels.isEmpty()) return;
        BufferedImage biomeImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = biomeImage.createGraphics();
        g2d.drawImage(lightBackground, 0, 0, null);
        for (Point p : biomePixels) {
            biomeImage.setRGB(p.x, p.y, originalImage.getRGB(p.x, p.y));
        }
        g2d.dispose();
        String biomeFileName = String.format("%s/%s_biome_%s.png", outputDir, imageName, biomeName.replaceAll("\\s+", "_"));
        ImageIO.write(biomeImage, "PNG", new File(biomeFileName));
        logger.info(" -> Image de biome sauvegardée : " + biomeFileName);
    }

    private static void visualizeEcosystemsForBiome(String imageName, String biomeName, List<Point> biomePixels, BufferedImage lightBackground, String outputDir) throws IOException {
        if (biomePixels.size() < 20) {
            logger.fine("Skipping ecosystem detection for biome '" + biomeName + "' (trop peu de pixels: " + biomePixels.size() + ").");
            return;
        }
        logger.info("   -> Traitement des écosystèmes pour le biome : " + biomeName);
        DBSCAN dbscan = new DBSCAN(3, 50);
        double[][] points = biomePixels.stream().map(p -> new double[]{p.getX(), p.getY()}).toArray(double[][]::new);
        int[] labels = dbscan.cluster(points);
        Map<Integer, List<Point>> pixelsParEcosysteme = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] != 0) pixelsParEcosysteme.computeIfAbsent(labels[i], k -> new ArrayList<>()).add(biomePixels.get(i));
        }
        if (pixelsParEcosysteme.isEmpty()) {
            logger.warning("Aucun écosystème détecté pour le biome '" + biomeName + "' après clustering.");
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
        String ecosystemFileName = String.format("%s/%s_ecosystemes_%s.png", outputDir, imageName, biomeName.replaceAll("\\s+", "_"));
        ImageIO.write(ecosystemImage, "PNG", new File(ecosystemFileName));
        logger.info("   -> Carte des écosystèmes pour '" + biomeName + "' sauvegardée. (" + pixelsParEcosysteme.size() + " écosystèmes)");
    }
}

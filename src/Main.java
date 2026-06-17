import AlgoCluster.DBSCAN;
import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import flou.Flou;
import flou.FlouGausien;
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
    private static final int NB_COULEURS_PALETTE = 40; // Augmenté pour plus de détails
    private static final double SEUIL_SIMILARITE_COULEUR = 18.0; // Légèrement augmenté
    private static final String IMAGE_INPUT_PATH = "Images/Planete_1.jpg";
    private static final String OUTPUT_DIR = "resultats_analyse";

    // --- PARAMÈTRES DBSCAN MODE NORMAL (< SEUIL_MOYEN pixels) ---
    // DBSCAN direct sur tous les pixels, précis
    private static final double DBSCAN_EPS     = 4.0;
    private static final int    DBSCAN_MIN_PTS = 5;

    // --- SEUILS DE BASCULE ---
    private static final int SEUIL_MOYEN  =  75_000;   // < 75k  → mode normal
    private static final int SEUIL_GRAND  = 500_000;   // < 500k → mode grille fine
    // ≥ 500k → mode grille + sous-échantillonnage aléatoire

    // --- PARAMÈTRES MODE GRILLE FINE (biomes moyens : 75k–500k pixels) ---
    // Grille 800×800 : bonne précision, reste rapide
    private static final int    FAST_GRID_SIZE       = 800;
    private static final double DBSCAN_EPS_FAST      = 4.0;  // même eps qu'en normal, adapté à la grille
    private static final int    DBSCAN_MIN_PTS_FAST  = 3;

    // --- PARAMÈTRES MODE GRILLE + SOUS-ÉCHANTILLONNAGE (très grands biomes ≥ 500k) ---
    // On tire aléatoirement MAX_SAMPLE pixels, puis grille 1000×1000
    private static final int    MAX_SAMPLE            = 300_000;
    private static final int    HUGE_GRID_SIZE        = 1000;
    private static final double DBSCAN_EPS_HUGE       = 4.0;
    private static final int    DBSCAN_MIN_PTS_HUGE   = 3;
    private static final int PIXEL_THRESHOLD_FAST_MODE = 75000;
    private static final int FAST_MODE_GRID_SIZE = 300;


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
            logger.info("  - DBSCAN normal  (< " + SEUIL_MOYEN  + "px) : eps=" + DBSCAN_EPS     + " minPts=" + DBSCAN_MIN_PTS);

            long startTime = System.currentTimeMillis();
            runPipeline();
            long endTime = System.currentTimeMillis();
            logger.info("\n--- Pipeline terminé en " + (endTime - startTime) / 1000.0 + " secondes. ---");
            logger.info("Résultats disponibles dans le répertoire : '" + OUTPUT_DIR + "'");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Une erreur critique a interrompu le pipeline.", e);
        }
    }

    private static void setupLogger() throws IOException {
        logger.setUseParentHandlers(false);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);

        FileHandler fileHandler = new FileHandler("analyse.log");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        logger.setLevel(Level.INFO);
    }

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

        // Flou Gaussien avec un noyau de 5x5 et une distribution (sigma) de 1.5.
        // Cela préserve mieux les bords qu'un flou moyen en donnant plus de poids au pixel central.
        Flou flou = new FlouGausien(5, 5, 1.5);
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

        // Affichage de la taille de chaque biome pour information
        pixelsParBiome.forEach((name, pixels) ->
                logger.info(String.format("  Biome %-20s : %d pixels %s",
                        name, pixels.size(),
                        pixels.size() > PIXEL_THRESHOLD_FAST_MODE ? "[MODE RAPIDE]" : "[MODE NORMAL]"))
        );

        // --- 4. VISUALISATION DES BIOMES ET ÉCOSYSTÈMES (PARALLÉLISÉE) ---
        logger.info("\nÉtape 4: Génération des images de biomes et d'écosystèmes en parallèle...");
        BufferedImage lightBackground = createLightBackground(originalImage, 0.75f);
        List<Callable<Void>> visualizationTasks = new ArrayList<>();

        for (Map.Entry<String, List<Point>> entry : pixelsParBiome.entrySet()) {
            String biomeName = entry.getKey();
            List<Point> biomePixels = entry.getValue();

            Callable<Void> task = () -> {
                visualizeBiome(imageName, biomeName, biomePixels, lightBackground, originalImage, biomesDir.getPath(), paletteBiomes);
                visualizeEcosystemsForBiome(imageName, biomeName, biomePixels, lightBackground, ecosystemsDir.getPath());
                return null;
            };
            visualizationTasks.add(task);
        }

        executor.invokeAll(visualizationTasks);
        executor.shutdown();
    }

    // =========================================================================
    // MÉTHODE PRINCIPALE D'ÉCOSYSTÈMES — DISPATCH MODE NORMAL / RAPIDE
    // =========================================================================

    private static void visualizeEcosystemsForBiome(
            String imageName, String biomeName, List<Point> biomePixels,
            BufferedImage lightBackground, String outputDir) throws IOException {

        if (biomePixels.size() < DBSCAN_MIN_PTS) {
            logger.fine("Skipping '" + biomeName + "' (trop peu de pixels: " + biomePixels.size() + ").");
            return;
        }

        Map<Integer, List<Point>> pixelsParEcosysteme;

        if (biomePixels.size() > PIXEL_THRESHOLD_FAST_MODE) {
            // ---- MODE RAPIDE : sous-échantillonnage sur grille ----
            logger.info("   -> [MODE RAPIDE] Traitement de '" + biomeName + "' (" + biomePixels.size() + " pixels)...");
            pixelsParEcosysteme = clusterFastMode(biomePixels, lightBackground.getWidth(), lightBackground.getHeight());
        } else {
            // ---- MODE NORMAL : DBSCAN direct ----
            logger.info("   -> [MODE NORMAL] Traitement de '" + biomeName + "' (" + biomePixels.size() + " pixels)...");
            pixelsParEcosysteme = clusterNormalMode(biomePixels);
        }

        if (pixelsParEcosysteme == null || pixelsParEcosysteme.isEmpty()) {
            logger.warning("Aucun écosystème détecté pour le biome '" + biomeName + "' après clustering.");
            return;
        }

        // Dessin du résultat
        BufferedImage ecosystemImage = new BufferedImage(
                lightBackground.getWidth(), lightBackground.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = ecosystemImage.createGraphics();
        g2d.drawImage(lightBackground, 0, 0, null);
        Random rand = new Random(42); // seed fixe = couleurs reproductibles
        for (List<Point> ecosystemPixels : pixelsParEcosysteme.values()) {
            Color flashyColor = new Color(
                    rand.nextInt(200) + 55,
                    rand.nextInt(200) + 55,
                    rand.nextInt(200) + 55);
            for (Point p : ecosystemPixels) {
                ecosystemImage.setRGB(p.x, p.y, flashyColor.getRGB());
            }
        }
        g2d.dispose();

        String ecosystemFileName = String.format("%s/%s_ecosystemes_%s.png",
                outputDir, imageName, biomeName.replaceAll("\\s+", "_"));
        ImageIO.write(ecosystemImage, "PNG", new File(ecosystemFileName));
        logger.info("   -> Carte des écosystèmes pour '" + biomeName + "' sauvegardée. ("
                + pixelsParEcosysteme.size() + " écosystèmes)");
    }

    // =========================================================================
    // MODE NORMAL : DBSCAN classique (précis, pour les petits biomes)
    // =========================================================================

    private static Map<Integer, List<Point>> clusterNormalMode(List<Point> biomePixels) {
        DBSCAN dbscan = new DBSCAN(DBSCAN_EPS, DBSCAN_MIN_PTS);
        double[][] points = biomePixels.stream()
                .map(p -> new double[]{p.getX(), p.getY()})
                .toArray(double[][]::new);
        int[] labels = dbscan.cluster(points);

        Map<Integer, List<Point>> result = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] != 0) {
                result.computeIfAbsent(labels[i], k -> new ArrayList<>()).add(biomePixels.get(i));
            }
        }
        return result;
    }

    // =========================================================================
    // MODE RAPIDE : grille → DBSCAN → projection (pour les grands biomes)
    // =========================================================================

    /**
     * Pour les grands biomes :
     * 1. Calcule la bounding box des pixels du biome.
     * 2. Crée une grille booléenne (occupé/libre) réduite à FAST_MODE_GRID_SIZE.
     * 3. Lance DBSCAN sur les cellules occupées de la grille (beaucoup moins de points).
     * 4. Projette le label de chaque cellule sur les pixels originaux.
     */
    private static Map<Integer, List<Point>> clusterFastMode(
            List<Point> biomePixels, int imgWidth, int imgHeight) {

        // 1. Bounding box
        int minX = imgWidth, maxX = 0, minY = imgHeight, maxY = 0;
        for (Point p : biomePixels) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }
        int bboxW = maxX - minX + 1;
        int bboxH = maxY - minY + 1;

        // 2. Calcul du facteur d'échelle pour tenir dans FAST_MODE_GRID_SIZE
        double scale = (double) FAST_MODE_GRID_SIZE / Math.max(bboxW, bboxH);
        int gridW = Math.max(1, (int) Math.ceil(bboxW * scale));
        int gridH = Math.max(1, (int) Math.ceil(bboxH * scale));

        // 3. Grille : chaque cellule = liste des pixels originaux qui y tombent
        @SuppressWarnings("unchecked")
        List<Point>[] cellPixels = new List[gridW * gridH];

        for (Point p : biomePixels) {
            int cx = Math.min((int) ((p.x - minX) * scale), gridW - 1);
            int cy = Math.min((int) ((p.y - minY) * scale), gridH - 1);
            int idx = cy * gridW + cx;
            if (cellPixels[idx] == null) cellPixels[idx] = new ArrayList<>();
            cellPixels[idx].add(p);
        }

        // 4. Collecte des cellules occupées → points pour DBSCAN
        List<double[]> gridPoints = new ArrayList<>();
        List<Integer> gridIndices = new ArrayList<>(); // index dans cellPixels
        for (int i = 0; i < cellPixels.length; i++) {
            if (cellPixels[i] != null) {
                int cx = i % gridW;
                int cy = i / gridW;
                gridPoints.add(new double[]{cx, cy});
                gridIndices.add(i);
            }
        }

        if (gridPoints.isEmpty()) return new HashMap<>();

        // 5. DBSCAN sur la grille réduite (très rapide)
        DBSCAN dbscan = new DBSCAN(DBSCAN_EPS_FAST, DBSCAN_MIN_PTS_FAST);
        double[][] pointsArray = gridPoints.toArray(new double[0][]);
        int[] labels = dbscan.cluster(pointsArray);

        // 6. Projection : label de cellule → pixels originaux
        Map<Integer, List<Point>> result = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] != 0) {
                int cellIdx = gridIndices.get(i);
                result.computeIfAbsent(labels[i], k -> new ArrayList<>())
                        .addAll(cellPixels[cellIdx]);
            }
        }
        return result;
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES (inchangées)
    // =========================================================================

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

    private static Map<String, List<Point>> mapBiomesParallel(
            BufferedImage blurredImg, Palette palette, ExecutorService executor)
            throws InterruptedException, ExecutionException {

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
            future.get().forEach((biomeName, points) ->
                    finalMap.merge(biomeName, points, (l1, l2) -> { l1.addAll(l2); return l1; }));
        }
        return finalMap;
    }

    private static BufferedImage createLightBackground(BufferedImage original, float percentage) {
        BufferedImage lightImage = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color c = new Color(original.getRGB(x, y));
                int newR = Math.round(c.getRed()   + percentage * (255 - c.getRed()));
                int newG = Math.round(c.getGreen() + percentage * (255 - c.getGreen()));
                int newB = Math.round(c.getBlue()  + percentage * (255 - c.getBlue()));
                lightImage.setRGB(x, y, new Color(newR, newG, newB).getRGB());
            }
        }
        return lightImage;
    }

    private static void visualizeBiome(
        String imageName, String biomeName, List<Point> biomePixels,
        BufferedImage lightBackground, BufferedImage originalImage, String outputDir, Palette palette) throws IOException {

        if (biomePixels.isEmpty()) return;

        // Récupérer la couleur uniforme du biome depuis la palette
        Color biomeColor = palette.getBiomeColor(biomeName);
        if (biomeColor == null) {
            logger.warning("Couleur non trouvée pour le biome: " + biomeName + ". Utilisation du noir par défaut.");
            biomeColor = Color.BLACK;
        }

        BufferedImage biomeImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = biomeImage.createGraphics();
        g2d.drawImage(lightBackground, 0, 0, null);

        // Dessiner les pixels du biome avec sa couleur uniforme
        g2d.setColor(biomeColor);
        for (Point p : biomePixels) {
            biomeImage.setRGB(p.x, p.y, biomeColor.getRGB());
        }
        g2d.dispose();

        String biomeFileName = String.format("%s/%s_biome_%s.png",
                outputDir, imageName, biomeName.replaceAll("\\s+", "_"));
        ImageIO.write(biomeImage, "PNG", new File(biomeFileName));
        logger.info(" -> Image de biome sauvegardée : " + biomeFileName);
    }
}

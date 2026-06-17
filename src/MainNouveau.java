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

public class MainNouveau {

    // --- PARAMÈTRES ---
    static final int NB_THREADS = Runtime.getRuntime().availableProcessors(); //utilise les cœurs disponibles
    static final double SEUIL_SIMILARITE = 15.0;
    static final int NB_COULEURS_PALETTE = 20;
    static final String IMAGE_INPUT_PATH = "Images/Planete_1.jpg";
    static final String OUTPUT_DIR = "rendus_individuels";

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        System.out.println("--- Démarrage du pipeline de visualisation parallèle ---");
        System.out.println("Utilisation de " + NB_THREADS + " threads.");

        // --- 1. ÉTAPES GLOBALES  ---
        File inputFile = new File(IMAGE_INPUT_PATH);
        if (!inputFile.exists()) {
            System.out.println("Erreur : Fichier d'entrée introuvable.");
            return;
        }
        BufferedImage originalImage = ImageIO.read(inputFile);
        String imageName = inputFile.getName().split("\\.")[0];

        // Création des dossiers de sortie
        new File(OUTPUT_DIR).mkdirs();
        new File(OUTPUT_DIR + "/" + imageName + "_biomes").mkdirs();
        new File(OUTPUT_DIR + "/" + imageName + "_ecosystemes_par_biome").mkdirs();

        Flou flou = new FlouMoyenne(3);
        BufferedImage blurredImg = flou.appliquerFlou(inputFile);
        System.out.println("Image pré-traitée avec un filtre de flou.");

        NormeCouleurs norme = new NormeBetterCIELAB();
        AlgoExtractionPalette kmeans = new PaletteKmeans(NB_COULEURS_PALETTE);
        Color[] paletteCouleurs = kmeans.extrairePalette(blurredImg, NB_COULEURS_PALETTE, norme);
        Palette paletteBiomes = new Palette(new BiomeMapper(norme).getBiomeMapping(paletteCouleurs), norme);
        System.out.println("Palette de biomes globale créée.");

        // --- 2. PARALLÉLISATION DE LA CARTOGRAPHIE DES BIOMES ---
        System.out.println("Cartographie des biomes en parallèle...");
        ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
        Map<String, List<Point>> pixelsParBiome = mapBiomesParallel(blurredImg, paletteBiomes, executor);
        System.out.println("Cartographie des biomes terminée.");

        // --- 3. PARALLÉLISATION DE L'ANALYSE ET DE LA VISUALISATION ---
        System.out.println("\nGénération des images de biomes et écosystèmes en parallèle...");
        BufferedImage lightBackground = createLightBackground(originalImage, 0.75f);
        List<Callable<Void>> visualizationTasks = new ArrayList<>();

        for (Map.Entry<String, List<Point>> entry : pixelsParBiome.entrySet()) {
            String biomeName = entry.getKey();
            List<Point> biomePixels = entry.getValue();

            Callable<Void> task = () -> {
                // Tâche A: Visualiser le biome sur fond clair
                visualizeBiome(imageName, biomeName, biomePixels, lightBackground, originalImage);
                // Tâche B: Visualiser les écosystèmes de ce biome avec des couleurs flashy
                visualizeEcosystemsForBiome(imageName, biomeName, biomePixels, lightBackground);
                return null;
            };
            visualizationTasks.add(task);
        }

        executor.invokeAll(visualizationTasks);
        executor.shutdown();
        System.out.println("\n--- Pipeline de visualisation terminé. Résultats dans le dossier '" + OUTPUT_DIR + "' ---");
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
        System.out.println(" -> Image de biome sauvegardée : " + biomeFileName);
    }

    private static void visualizeEcosystemsForBiome(String imageName, String biomeName, List<Point> biomePixels, BufferedImage lightBackground) throws IOException {
        if (biomePixels.size() < 20) return;

        System.out.println("Traitement des écosystèmes pour le biome : " + biomeName);
        DBSCAN dbscan = new DBSCAN(3, 50);
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

        // Création de l'image unique pour tous les écosystèmes de ce biome
        BufferedImage ecosystemImage = new BufferedImage(lightBackground.getWidth(), lightBackground.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = ecosystemImage.createGraphics();
        g2d.drawImage(lightBackground, 0, 0, null);

        Random rand = new Random();
        for (List<Point> ecosystemPixels : pixelsParEcosysteme.values()) {
            // Couleur aléatoire et flashy pour chaque écosystème
            Color flashyColor = new Color(rand.nextInt(200) + 55, rand.nextInt(200) + 55, rand.nextInt(200) + 55);
            for (Point p : ecosystemPixels) {
                ecosystemImage.setRGB(p.x, p.y, flashyColor.getRGB());
            }
        }
        g2d.dispose();

        String ecosystemFileName = String.format("%s/%s_ecosystemes_par_biome/%s_ecosystemes_%s.png", OUTPUT_DIR, imageName, imageName, biomeName);
        ImageIO.write(ecosystemImage, "PNG", new File(ecosystemFileName));
        System.out.println(" -> Carte des écosystèmes pour '" + biomeName + "' sauvegardée. (" + pixelsParEcosysteme.size() + " écosystèmes)");
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

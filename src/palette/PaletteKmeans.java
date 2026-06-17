package palette;

import Norme.NormeCouleurs;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// Mise à jour pour implémenter la nouvelle interface standardisée
public class PaletteKmeans implements AlgoExtractionPalette {

    private final int k; // Nombre de clusters (nbCouleurs)

    public PaletteKmeans(int k) {
        this.k = k;
    }

    // Getter pour l'accès au paramètre
    public int getK() {
        return k;
    }

    @Override
    public Color[] extrairePalette(BufferedImage image, int nbCouleurs, NormeCouleurs norme) {
        // Le nombre de couleurs est maintenant défini par le constructeur
        if (nbCouleurs != 0 && nbCouleurs != this.k) {
            System.out.println("Avertissement : Le nombre de couleurs demandé (" + nbCouleurs + ") est différent de celui configuré (" + this.k + "). Utilisation de k=" + this.k);
        }

        final int MAX_ITERATIONS = 100;
        final int RESIZED_WIDTH = 100;
        final int RESIZED_HEIGHT = 100;

        // Redimensionner l'image pour l'optimisation
        Image scaledImage = image.getScaledInstance(RESIZED_WIDTH, RESIZED_HEIGHT, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(RESIZED_WIDTH, RESIZED_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        Color[] palette = new Color[this.k];
        Random rand = new Random();

        // Initialisation des centroïdes avec des pixels aléatoires
        for (int i = 0; i < this.k; i++) {
            int randomX = rand.nextInt(RESIZED_WIDTH);
            int randomY = rand.nextInt(RESIZED_HEIGHT);
            palette[i] = new Color(resizedImage.getRGB(randomX, randomY));
        }

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Map<Color, ArrayList<Color>> groupement = new HashMap<>();
            for (Color c : palette) {
                groupement.put(c, new ArrayList<>());
            }

            // Attribution de chaque pixel au centroïde le plus proche
            for (int y = 0; y < RESIZED_HEIGHT; y++) {
                for (int x = 0; x < RESIZED_WIDTH; x++) {
                    Color pixelColor = new Color(resizedImage.getRGB(x, y));
                    double distanceMin = Double.MAX_VALUE;
                    Color nearestCentroid = palette[0];
                    for (Color centroid : palette) {
                        double distance = norme.distanceCouleur(pixelColor, centroid);
                        if (distance < distanceMin) {
                            nearestCentroid = centroid;
                            distanceMin = distance;
                        }
                    }
                    groupement.computeIfAbsent(nearestCentroid, key -> new ArrayList<>()).add(pixelColor);
                }
            }

            Color[] nouvellePalette = new Color[this.k];
            boolean hasChanged = false;

            // Recalcul des centroïdes
            for (int i = 0; i < this.k; i++) {
                ArrayList<Color> couleursDuGroupe = groupement.get(palette[i]);
                if (couleursDuGroupe != null && !couleursDuGroupe.isEmpty()) {
                    long sommeR = 0, sommeG = 0, sommeB = 0;
                    for (Color c : couleursDuGroupe) {
                        sommeR += c.getRed();
                        sommeG += c.getGreen();
                        sommeB += c.getBlue();
                    }
                    nouvellePalette[i] = new Color(
                            (int) (sommeR / couleursDuGroupe.size()),
                            (int) (sommeG / couleursDuGroupe.size()),
                            (int) (sommeB / couleursDuGroupe.size())
                    );
                } else {
                    // Si un centroïde n'a aucun point, on le réinitialise aléatoirement
                    int randomX = rand.nextInt(RESIZED_WIDTH);
                    int randomY = rand.nextInt(RESIZED_HEIGHT);
                    nouvellePalette[i] = new Color(resizedImage.getRGB(randomX, randomY));
                }
                if (!palette[i].equals(nouvellePalette[i])) {
                    hasChanged = true;
                }
            }

            if (!hasChanged) {
                // System.out.println("K-Means a convergé après " + (iter + 1) + " itérations.");
                break;
            }
            palette = nouvellePalette;
        }

        return palette;
    }

    @Override
    public String toString() {
        return "K-Means (k=" + k + ")";
    }
}

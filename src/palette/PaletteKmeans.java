package palette;


import Norme.NormeCouleurs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PaletteKmeans implements ExtractionPalette {
    public Color[] extrairePalette(BufferedImage image, int nbCouleurs, NormeCouleurs norme) {
        final int MAX_ITERATIONS = 50;
        Color[] palette = new Color[nbCouleurs];

        //creation de la palette d'origine
        for (int i = 0; i < palette.length; i++) {
            palette[i] = new Color(
                    (int) (Math.random() * 255),
                    (int) (Math.random() * 255),
                    (int) (Math.random() * 255)
            );
        }

        // Boucle de convergence
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Map<Color, ArrayList<Color>> groupement = new HashMap<>();
            for (Color c : palette) {
                groupement.put(c, new ArrayList<>());
            }

            //atribution de chaque pixel à la couleur la plus proche
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    Color pixelColor = new Color(image.getRGB(x, y));
                    double distanceMin = Double.MAX_VALUE;
                    Color nearestColor = palette[0];
                    for (Color centroid : palette) {
                        double distance = norme.distanceCouleur(pixelColor, centroid);
                        if (distance < distanceMin) {
                            nearestColor = centroid;
                            distanceMin = distance;
                        }
                    }
                    groupement.get(nearestColor).add(pixelColor);
                }
            }

            // moyenne des groupes pour former la nouvelle palette
            Color[] nouvellePalette = new Color[nbCouleurs];
            for (int i = 0; i < palette.length; i++) {
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
                    nouvellePalette[i] = new Color(
                            (int) (Math.random() * 255),
                            (int) (Math.random() * 255),
                            (int) (Math.random() * 255)
                    );
                }
            }

            // si la palette ne change pas on arrete
            if (Arrays.equals(palette, nouvellePalette)) {
                System.out.println("K-Means a convergé après " + (iter + 1) + " itérations.");
                break;
            }
            // Sinon, on recommence sur la nouvelle palette
            palette = nouvellePalette;
        }

        return palette;
    }

    @Override
    public String toString() {
        return "PaletteKmeans";
    }
}

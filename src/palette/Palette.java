package palette;

import Norme.NormeCouleurs;

import java.awt.*;

public class Palette {

    private final Color[] colors;
    private final NormeCouleurs norme;

    public Palette(Color[] colors, NormeCouleurs norme) {
        this.colors = colors;
        this.norme = norme;
    }

    public Color getPlusProche(Color c){
        if (colors == null || colors.length == 0) {
            return Color.BLACK; // Ou une autre couleur par défaut
        }

        Color nearestColor = colors[0];
        double minDistance = norme.distanceCouleur(c, nearestColor);

        for (int i = 1; i < colors.length; i++) {
            Color c2 = colors[i];
            if (c2 == null) continue;
            double distance = norme.distanceCouleur(c, c2);
            if (distance < minDistance) {
                nearestColor = c2;
                minDistance = distance;
            }
        }
        return nearestColor;
    }

    public Color[] getColors() {
        return colors;
    }
}

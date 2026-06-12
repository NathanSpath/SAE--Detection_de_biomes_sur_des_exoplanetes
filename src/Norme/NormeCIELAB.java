package Norme;

import java.awt.*;

public class NormeCIELAB implements NormeCouleurs {
    
    @Override
    public double distanceCouleur(Color c1, Color c2) {
        int[] lab1 = OutilsCouleur.rgb2lab(c1.getRed(), c1.getGreen(), c1.getBlue());
        int[] lab2 = OutilsCouleur.rgb2lab(c2.getRed(), c2.getGreen(), c2.getBlue());
        return Math.sqrt(
                Math.pow(lab2[0]-lab1[0],2) +
                        Math.pow(lab2[1]-lab1[1],2) +
                        Math.pow(lab2[2]-lab1[2],2)
        );

    }

    @Override
    public String toString() {
        return "NormeCIELAB";
    }
}

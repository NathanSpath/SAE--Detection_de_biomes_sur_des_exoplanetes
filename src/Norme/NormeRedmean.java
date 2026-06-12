package Norme;

import java.awt.*;

public class NormeRedmean implements NormeCouleurs {

    @Override
    public double distanceCouleur(Color c1, Color c2) {
        double r = (double) 1 /2*(c1.getRed()+c2.getRed());
        double deltaR = c1.getRed() - c2.getRed();
        double deltaG = c1.getGreen() - c2.getGreen();
        double deltaB = c1.getBlue() - c2.getBlue();

        return Math.sqrt(
                (2+r/1256) *
                        Math.pow(deltaR,2)+
                        4*Math.pow(deltaG,2)+
                        (2+(255-r)/256)*
                                Math.pow(deltaB,2)
        );
    }


    @Override
    public String toString() {
        return "NormeRedMean";
    }
}

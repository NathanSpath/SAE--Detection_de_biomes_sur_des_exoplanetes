package Norme;

import java.awt.*;

public class NormeBasique implements NormeCouleurs{

    @Override
    public double distanceCouleur(Color c1, Color c2) {
        return (Math.pow(c1.getRed()-c2.getRed(),2)+
                Math.pow(c1.getGreen()-c2.getGreen(),2)+
                Math.pow(c1.getBlue()-c2.getBlue(),2));
    }

    @Override
    public String toString() {
        return "Normebasique";
    }
}

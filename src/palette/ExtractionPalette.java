package palette;
import Norme.NormeCouleurs;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface ExtractionPalette {
    public Color[] extrairePalette(BufferedImage image, int nbCouleurs, NormeCouleurs normeCouleurs);
    }

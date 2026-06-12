package flou;
import java.awt.*;

public interface Flou {

    /**
     * Interface de methode de flou
     * @param x
     * @param y
     * @param distribution écart type gausien
     */
    public void appliquerFlou(int x, int y, double distribution, Image image);
}

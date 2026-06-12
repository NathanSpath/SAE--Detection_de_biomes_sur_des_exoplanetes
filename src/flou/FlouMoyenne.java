package flou;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlouMoyenne implements Flou{
    int TAILLE_FILTRE;
    public FlouMoyenne(int taille){
        this.TAILLE_FILTRE = taille;
    }
    @Override
    public BufferedImage appliquerFlou(File imgFile) throws IOException {
        //cree l'image a partir du fichier
        BufferedImage image = ImageIO.read(imgFile);
        BufferedImage imageReconstruite = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        //on parcours l'image
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                List<Color> pixels = new ArrayList<>();
                //pour chaque pixel on prend sa valeur et celle de autour de lui
                for (int j = -TAILLE_FILTRE; j <= TAILLE_FILTRE; j++) {
                    for (int i = -TAILLE_FILTRE; i <= TAILLE_FILTRE; i++) {
                        int currentX = x + i;
                        int currentY = y + j;
                        // on verifie que le pixel est dans l'image
                        if (currentX >= 0 && currentX < image.getWidth() && currentY >= 0 && currentY < image.getHeight()) {
                            pixels.add(new Color(image.getRGB(currentX, currentY)));
                        }
                    }
                }

                //on calcule la moyenne des valeur de couleur ces N case
                int r = 0;
                int g = 0;
                int b = 0;
                for (Color c : pixels) {
                    r += c.getRed();
                    g += c.getGreen();
                    b += c.getBlue();
                }
                r /= pixels.size();
                g /= pixels.size();
                b /= pixels.size();
                Color moyenne = new Color(r,g,b);
                //on remplace les couleur par la moyenne a ces N case
                imageReconstruite.setRGB(x,y,moyenne.getRGB());
            }
        }
        return imageReconstruite;
    }
}

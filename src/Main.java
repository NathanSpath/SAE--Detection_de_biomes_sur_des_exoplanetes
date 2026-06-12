import flou.Flou;
import flou.FlouMoyenne;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        File img = new File(args[0]);
        Flou methodeFlou = new FlouMoyenne();
        BufferedImage imageTraitee = methodeFlou.appliquerFlou(img);
        ImageIO.write(imageTraitee,"jpg", new File("imagesTraitées/imageTraitee.jpg"));
    }

}

package flou;
import java.awt.Image;

public class FlouGausien implements  Flou{



    @Override
    public void appliquerFlou(Image image) {
        System.out.println("Application du flou gausien sur l'image");
    }
}

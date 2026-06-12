package flou;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FlouGausien implements  Flou{

    private final int x;
    private final int y;
    private final double distribution;

    public FlouGausien(int x, int y, double distribution) {
        this.x = x;
        this.y = y;
        this.distribution = distribution;
    }

    private double[][] genererFiltreGausien(){
        double[][] filtreGausien = new double[this.x][this.y];

        //Initialisation des valeurs
        double somme = 0;
        int rayonX = this.x / 2;
        int rayonY = this.y / 2;
        double pi2sigmaCarre

    }


    @Override
    public void appliquerFlou(File image) throws IOException {
        System.out.println("Application du flou gausien sur l'image");

        BufferedImage img = ImageIO.read(image);
        int width = img.getWidth();
        int height = img.getHeight();

        for(int i=0; i<height; i++){
            for(int j=0;j<width;j++){

            }
        }
    }
}

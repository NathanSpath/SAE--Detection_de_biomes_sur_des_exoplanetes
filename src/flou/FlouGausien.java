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

    /**
     * Génère un filtre gausien de taille x*y avec une distribution donnée
     * @return la matrice lié au filtre gausien
     */
    private double[][] genererFiltreGausien(){
        double[][] filtreGausien = new double[this.x][this.y];

        //Initialisation des valeurs
        double somme = 0;
        int rayonX = this.x / 2;
        int rayonY = this.y / 2;
        double deuxSigmaCarre =  2 * Math.pow(this.distribution, 2);
        double pi2SigmaCarre = Math.PI * deuxSigmaCarre;

        //Calcul des valeurs selon G(x,y)
        for (int y = -rayonY; y <= rayonY; y++) {
            for (int x = -rayonX; x <= rayonX; x++) {
                double exposant = -(Math.pow(x, 2) + Math.pow(y, 2)) / deuxSigmaCarre;
                double valeur = (1 / pi2SigmaCarre) * Math.exp(exposant);

                filtreGausien[y + rayonY][x + rayonX] = valeur;
                somme += valeur;
            }
        }

        //Normalisation : la somme de tous les coefficients doit être exactement égale à 1
        for(int i=0; i < this.y; i++) {
            for(int j=0; j < this.x; j++) {
                filtreGausien[j][i] /= somme;
            }
        }
        return filtreGausien;
    }

    /**
     * Méthode qui applique le flou Gausien à l'image
     * @param image image à flouter
     * @return l'image flouté
     * @throws IOException
     */
    @Override
    public void appliquerFlou(File image) throws IOException {
        System.out.println("Application du flou gausien sur l'image");

        BufferedImage img = ImageIO.read(image);
        int width = img.getWidth();
        int height = img.getHeight();

        BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        //On récupère la matrice du filtre gausien
        double[][] matrice = genererFiltreGausien();
        int rayonX = this.x / 2;
        int rayonY = this.y / 2;

        //Parcours de l'image
        for(int i=0; i<height; i++){
            for(int j=0;j<width;j++){
                double rouge = 0;
                double vert = 0;
                double bleu = 0;

                //Parcours des voisins selon la taille du filtre
                for(int ky = -rayonY; ky <= rayonY; ky++) {
                    for(int kx = -rayonX; kx <= rayonX; kx++) {

                        //Gestion des bords : on utilise la technique de "clamping" pour éviter les dépassements d'index
                        int pixelY = Math.min(Math.max(i + ky,0), height - 1);
                        int pixelX = Math.min(Math.max(j + kx,0), width - 1);

                        //Récupération de la couleur des voisins
                        int rgb = img.getRGB(pixelX, pixelY);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        //Application du coefficient du kernel
                        double weight = matrice[ky + rayonY][kx + rayonX];
                        rouge += r * weight;
                        vert += g * weight;
                        bleu += b * weight;
                    }
                }

                //Recomposition du pixel final et insertion dans l'image de destination
                int finalR = Math.min(Math.max((int) Math.round(rouge), 0), 255);
                int finalG = Math.min(Math.max((int) Math.round(vert), 0), 255);
                int finalB = Math.min(Math.max((int) Math.round(bleu), 0), 255);

                int newRgb = (finalR << 16) | (finalG << 8) | finalB;
                newImg.setRGB(j, i, newRgb);
            }
        }
        return newImg;
    }
}

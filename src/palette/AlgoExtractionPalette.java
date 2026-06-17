package palette;

import Norme.NormeCouleurs;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Interface pour les algorithmes qui extraient une palette de couleurs d'une image.
 */
public interface AlgoExtractionPalette {

    /**
     * Extrait une palette de couleurs d'une image donnée.
     *
     * @param image L'image à analyser.
     * @param nbCouleurs Le nombre de couleurs à extraire.
     * @param norme La norme de couleur à utiliser pour la comparaison.
     * @return Un tableau de {@link Color} représentant la palette.
     */
    Color[] extrairePalette(BufferedImage image, int nbCouleurs, NormeCouleurs norme);

    /**
     * Retourne une représentation textuelle de l'algorithme et de ses paramètres.
     * @return Une chaîne de caractères décrivant l'algorithme.
     */
    @Override
    String toString();
}
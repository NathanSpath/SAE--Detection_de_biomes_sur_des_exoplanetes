package Norme;

import java.awt.*;

public class NormeBetterCIELAB implements NormeCouleurs{
    @Override
    public double distanceCouleur(Color c1, Color c2) {
        // Comme dans NormeCIELAB, on copie la méthode rgb2lab pour éviter l'erreur de package par défaut.
        int[] lab1 = OutilsCouleur.rgb2lab(c1.getRed(), c1.getGreen(), c1.getBlue());
        int[] lab2 = OutilsCouleur.rgb2lab(c2.getRed(), c2.getGreen(), c2.getBlue());
        
        double deltaL = lab1[0] - lab2[0]; // ∆L = L*1 − L*2

        // C1 = sqrt(a*1^2 + b*1^2) (Erreur dans le code original: c'était un - au lieu d'un +)
        double c1_val = Math.sqrt(Math.pow(lab1[1], 2) + Math.pow(lab1[2], 2));
        
        // C2 = sqrt(a*2^2 + b*2^2)
        double c2_val = Math.sqrt(Math.pow(lab2[1], 2) + Math.pow(lab2[2], 2));
        
        double deltaC = c1_val - c2_val;

        // ∆H = sqrt( (a*1 − a*2)^2 + (b*1 − b*2)^2 - ∆C^2 )
        // Attention aux valeurs négatives sous la racine à cause des imprécisions des flottants.
        double deltaH_squared = Math.pow(lab1[1] - lab2[1], 2) + Math.pow(lab1[2] - lab2[2], 2) - Math.pow(deltaC, 2);
        // On s'assure que ce ne soit pas négatif avant la racine (ce qui peut arriver avec double)
        double deltaH = deltaH_squared > 0 ? Math.sqrt(deltaH_squared) : 0;

        double sc = 1 + 0.045 * c1_val;
        double sh = 1 + 0.015 * c1_val;

        return Math.sqrt(
                Math.pow(deltaL, 2) +
                Math.pow(deltaC / sc, 2) +
                Math.pow(deltaH / sh, 2)
        );
    }


    @Override
    public String toString() {
        return "NormeBetterCIELAB";
    }
}

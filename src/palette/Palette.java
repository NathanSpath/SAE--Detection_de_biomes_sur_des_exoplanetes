package palette;

import Norme.NormeCouleurs;

import java.awt.*;
import java.util.Map;
import java.util.HashMap;

public class Palette {

    private final Map<String, Color> biomeColors;
    private final NormeCouleurs norme;

    /**
     * Crée une nouvelle palette à partir d'une carte de noms de biomes et de leurs couleurs associées.
     * @param biomeColors Une carte associant les noms de biomes à leurs couleurs.
     * @param norme La norme de couleur à utiliser pour les calculs de distance.
     */
    public Palette(Map<String, Color> biomeColors, NormeCouleurs norme) {
        // Crée une copie défensive de la carte pour éviter les modifications externes inattendues.
        this.biomeColors = new HashMap<>(biomeColors);
        this.norme = norme;
    }

    /**
     * Retourne le nom du biome dont la couleur est la plus proche de la couleur cible donnée.
     * @param couleurCible La couleur pour laquelle trouver le biome le plus proche.
     * @return Le nom (String) du biome le plus proche. Retourne "Inconnu" si la palette est vide.
     */
    public String getBiomePlusProche(Color couleurCible) {
        if (biomeColors.isEmpty()) {
            return "Inconnu";
        }

        String nearestBiomeName = "Inconnu";
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Color> entry : biomeColors.entrySet()) {
            double distance = norme.distanceCouleur(couleurCible, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                nearestBiomeName = entry.getKey();
            }
        }
        return nearestBiomeName;
    }

    /**
     * Retourne la carte complète des biomes et de leurs couleurs.
     * @return Une carte non modifiable des noms de biomes et de leurs couleurs.
     */
    public Map<String, Color> getBiomeColors() {
        return biomeColors;
    }

    /**
     * Retourne le nombre de biomes dans la palette.
     * @return Le nombre de biomes.
     */
    public int getNbBiomes() {
        return biomeColors.size();
    }
}

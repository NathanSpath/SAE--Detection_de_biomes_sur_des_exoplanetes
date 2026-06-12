package palette;

import Norme.NormeCouleurs;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class BiomeMapper {

    private final Map<Color, String> biomeReference;
    private final NormeCouleurs norme;

    public BiomeMapper(NormeCouleurs norme) {
        this.norme = norme;
        this.biomeReference = new HashMap<>();
        //initialisation de la palette exmple du sujet pour comparaison avec la notre
        biomeReference.put(new Color(71, 70, 61), "Tundra");
        biomeReference.put(new Color(43, 50, 35), "Taïga");
        biomeReference.put(new Color(59, 66, 43), "Forêt tempérée");
        biomeReference.put(new Color(46, 64, 34), "Forêt tropicale");
        biomeReference.put(new Color(84, 106, 70), "Savane");
        biomeReference.put(new Color(104, 95, 82), "Prairie");
        biomeReference.put(new Color(152, 140, 120), "Désert");
        biomeReference.put(new Color(200, 200, 200), "Glacier");
        biomeReference.put(new Color(49, 83, 100), "Eau peu profonde");
        biomeReference.put(new Color(12, 31, 47), "Eau profonde");
    }

    public String getBiomeForColor(Color color) {
        double minDistance = Double.MAX_VALUE;
        String closestBiome = "Inconnu";

        for (Map.Entry<Color, String> entry : biomeReference.entrySet()) {
            double distance = norme.distanceCouleur(color, entry.getKey());
            if (distance < minDistance) {
                minDistance = distance;
                closestBiome = entry.getValue();
            }
        }
        return closestBiome;
    }
}

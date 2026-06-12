package palette;

import Norme.NormeCouleurs;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BiomeMapper {

    private final Map<Color, String> biomeReference;
    private final NormeCouleurs norme;

    public BiomeMapper(NormeCouleurs norme) {
        this.norme = norme;
        this.biomeReference = new HashMap<>();
        // Initialisation de la palette de référence
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

    private static class Match implements Comparable<Match> {
        final Color paletteColor;
        final int paletteIndex;
        final String biomeName;
        final double distance;

        Match(Color paletteColor, int paletteIndex, String biomeName, double distance) {
            this.paletteColor = paletteColor;
            this.paletteIndex = paletteIndex;
            this.biomeName = biomeName;
            this.distance = distance;
        }

        @Override
        public int compareTo(Match other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    /**
     * Attribue à chaque couleur de la palette un biome unique en trouvant la meilleure correspondance globale.
     * @param palette Le tableau de couleurs à mapper.
     * @return Une carte associant un nom de biome à une couleur à la carte contiendra autant d'entrées que la palette d'entrée.
     */
    public Map<String, Color> getBiomeMapping(Color[] palette) {
        List<Match> allPossibleMatches = new ArrayList<>();

        //créer une liste de tous les appariements possibles entre les couleurs de la palette et les biomes de référence.
        for (int i = 0; i < palette.length; i++) {
            for (Map.Entry<Color, String> refEntry : biomeReference.entrySet()) {
                double dist = norme.distanceCouleur(palette[i], refEntry.getKey());
                allPossibleMatches.add(new Match(palette[i], i, refEntry.getValue(), dist));
            }
        }

        //trier la liste
        Collections.sort(allPossibleMatches);

        Map<String, Color> finalMapping = new HashMap<>();
        Set<Integer> usedPaletteIndexes = new HashSet<>();
        Set<String> usedBiomeNames = new HashSet<>();

        //itérer à travers la liste triée de tous les appariements possibles.
        for (Match match : allPossibleMatches) {
            // Vérifier si le biome et la couleur de la palette ont déjà été utilisés.
            if (!usedBiomeNames.contains(match.biomeName) && !usedPaletteIndexes.contains(match.paletteIndex)) {
                finalMapping.put(match.biomeName, match.paletteColor);
                usedBiomeNames.add(match.biomeName);
                usedPaletteIndexes.add(match.paletteIndex);
            }

            //si toute les couleur on un biome on s'arrete
            if (finalMapping.size() == palette.length) {
                break;
            }
        }

        return finalMapping;
    }
}

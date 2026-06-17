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

        // Palette de référence optimisée avec des couleurs plus distinctes
        // Biomes aquatiques
        biomeReference.put(new Color(15, 25, 80), "Ocean_Profond");       // Bleu plus saturé
        biomeReference.put(new Color(40, 100, 180), "Mer_Peu_Profonde");

        // Biomes terrestres et arides
        biomeReference.put(new Color(210, 180, 140), "Desert_Sable");
        biomeReference.put(new Color(139, 90, 43), "Terre_Rocheuse");      // Marron plus clair
        biomeReference.put(new Color(110, 110, 110), "Montagne_Rocheuse"); // Gris plus clair

        // Biomes de végétation
        biomeReference.put(new Color(34, 139, 34), "Foret_Dense");
        biomeReference.put(new Color(154, 205, 50), "Plaine_Verdoyante");
        biomeReference.put(new Color(85, 107, 47), "Savane_Seche");

        // Biomes volcaniques et glaciaires
        biomeReference.put(new Color(50, 45, 45), "Terre_Volcanique");    // Marron très foncé, pas noir
        biomeReference.put(new Color(245, 245, 245), "Calotte_Glaciaire");
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

    public Map<String, Color> getBiomeMapping(Color[] palette) {
        return getBiomeMapping(palette, palette.length);
    }

    public Map<String, Color> getBiomeMapping(Color[] palette, int n) {
        List<Match> allPossibleMatches = new ArrayList<>();

        for (int i = 0; i < palette.length; i++) {
            for (Map.Entry<Color, String> refEntry : biomeReference.entrySet()) {
                double dist = norme.distanceCouleur(palette[i], refEntry.getKey());
                allPossibleMatches.add(new Match(palette[i], i, refEntry.getValue(), dist));
            }
        }

        Collections.sort(allPossibleMatches);

        Map<String, Color> finalMapping = new HashMap<>();
        Set<Integer> usedPaletteIndexes = new HashSet<>();
        Set<String> usedBiomeNames = new HashSet<>();

        for (Match match : allPossibleMatches) {
            if (finalMapping.size() >= n) {
                break;
            }
            if (!usedBiomeNames.contains(match.biomeName) && !usedPaletteIndexes.contains(match.paletteIndex)) {
                finalMapping.put(match.biomeName, match.paletteColor);
                usedBiomeNames.add(match.biomeName);
                usedPaletteIndexes.add(match.paletteIndex);
            }
        }

        return finalMapping;
    }
}

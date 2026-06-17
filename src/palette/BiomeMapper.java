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

        // Noms plus génériques et couleurs plus représentatives.
        biomeReference.put(new Color(15, 23, 42),    "Eau_Profonde");
        biomeReference.put(new Color(60, 120, 160),  "Eau_Peu_Profonde");
        biomeReference.put(new Color(210, 200, 140), "Plage_Sable");
        biomeReference.put(new Color(190, 180, 110), "Desert");
        biomeReference.put(new Color(120, 150, 70),  "Plaine");
        biomeReference.put(new Color(60, 110, 50),   "Foret");
        biomeReference.put(new Color(30, 70, 40),    "Foret_Dense");
        biomeReference.put(new Color(140, 130, 120), "Toundra_Roche");
        biomeReference.put(new Color(90, 90, 95),    "Montagne");
        biomeReference.put(new Color(240, 245, 250), "Neige_Glacier");
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
     * @return Une carte associant un nom de biome à une couleur.
     */
    public Map<String, Color> getBiomeMapping(Color[] palette) {
        List<Match> allPossibleMatches = new ArrayList<>();

        // Créer une liste de tous les appariements possibles
        for (int i = 0; i < palette.length; i++) {
            for (Map.Entry<Color, String> refEntry : biomeReference.entrySet()) {
                double dist = norme.distanceCouleur(palette[i], refEntry.getKey());
                allPossibleMatches.add(new Match(palette[i], i, refEntry.getValue(), dist));
            }
        }

        // Trier les appariements du plus proche au plus lointain
        Collections.sort(allPossibleMatches);

        Map<String, Color> finalMapping = new HashMap<>();
        Set<Integer> usedPaletteIndexes = new HashSet<>();
        Set<String> usedBiomeNames = new HashSet<>();

        // Itérer pour trouver la meilleure attribution un-à-un
        for (Match match : allPossibleMatches) {
            if (!usedBiomeNames.contains(match.biomeName) && !usedPaletteIndexes.contains(match.paletteIndex)) {
                finalMapping.put(match.biomeName, match.paletteColor);
                usedBiomeNames.add(match.biomeName);
                usedPaletteIndexes.add(match.paletteIndex);
            }

            // Arrêter si toutes les couleurs de la palette ont été assignées
            if (finalMapping.size() == palette.length) {
                break;
            }
        }

        return finalMapping;
    }
}

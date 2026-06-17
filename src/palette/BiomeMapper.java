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

        // Biomes aquatiques
        biomeReference.put(new Color(15, 23, 42),    "Ocean_Profond");
        biomeReference.put(new Color(30, 60, 110),   "Ocean");
        biomeReference.put(new Color(60, 120, 160),  "Mer_Peu_Profonde");
        biomeReference.put(new Color(100, 160, 200), "Lac");
        biomeReference.put(new Color(130, 190, 220), "Rivière");

        // Biomes côtiers et arides
        biomeReference.put(new Color(210, 200, 140), "Plage_Sable");
        biomeReference.put(new Color(230, 220, 170), "Dunes_Sable");
        biomeReference.put(new Color(190, 180, 110), "Desert_Sable");
        biomeReference.put(new Color(160, 140, 90),  "Desert_Rocheux");
        biomeReference.put(new Color(200, 160, 120), "Badlands");

        // Biomes de végétation
        biomeReference.put(new Color(120, 150, 70),  "Plaine_Herbeuse");
        biomeReference.put(new Color(150, 170, 90),  "Prairie");
        biomeReference.put(new Color(100, 130, 60),  "Steppe");
        biomeReference.put(new Color(60, 110, 50),   "Foret_Temperee");
        biomeReference.put(new Color(30, 70, 40),    "Foret_Boreale_Dense");
        biomeReference.put(new Color(40, 90, 60),    "Foret_Tropicale");
        biomeReference.put(new Color(80, 120, 40),   "Jungle");
        biomeReference.put(new Color(100, 100, 40),  "Savane");
        biomeReference.put(new Color(140, 130, 80),  "Brousse");

        // Biomes montagneux et froids
        biomeReference.put(new Color(140, 130, 120), "Toundra_Roche");
        biomeReference.put(new Color(140, 145, 120), "Toundra_Herbeuse");
        biomeReference.put(new Color(90, 90, 95),    "Montagne_Basse");
        biomeReference.put(new Color(70, 70, 80),    "Haute_Montagne");
        biomeReference.put(new Color(200, 195, 190), "Pics_Rocheux");
        biomeReference.put(new Color(250, 250, 255), "Neige_Permanente");
        biomeReference.put(new Color(210, 225, 235), "Glacier");

        // Biomes spéciaux
        biomeReference.put(new Color(110, 130, 110), "Marais");
        biomeReference.put(new Color(80, 60, 50),    "Terre_Volcanique");
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
        return getBiomeMapping(palette, palette.length);
    }

    /**
     * Sélectionne les 'n' biomes les plus représentatifs pour la palette de couleurs donnée.
     * @param palette Le tableau de couleurs à mapper.
     * @param n Le nombre de biomes à sélectionner.
     * @return Une carte associant un nom de biome à une couleur.
     */
    public Map<String, Color> getBiomeMapping(Color[] palette, int n) {
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
            if (finalMapping.size() >= n) {
                break; // Arrêter une fois que 'n' biomes ont été trouvés
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

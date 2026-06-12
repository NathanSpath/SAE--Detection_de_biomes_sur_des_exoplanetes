import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import flou.Flou;
import flou.FlouMoyenne;
import palette.BiomeMapper;
import palette.ExtractionPalette;
import palette.PaletteKmeans;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {
    // On cherche plus de couleurs pour avoir plus de candidats
    static final int NB_COULEURS_CANDIDATES = 20;
    // Seuil de similarité pour le filtrage. Plus il est élevé, plus les couleurs seront différentes.
    static final double SEUIL_SIMILARITE = 20.0;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Veuillez fournir le chemin de l'image en argument.");
            return;
        }
        File img = new File(args[0]);
        Flou methodeFlou = new FlouMoyenne(5);
        ExtractionPalette extraction = new PaletteKmeans();
        NormeCouleurs norme = new NormeBetterCIELAB();
        BiomeMapper mapper = new BiomeMapper(norme);

        BufferedImage imageTraitee = methodeFlou.appliquerFlou(img);
        ImageIO.write(imageTraitee, "jpg", new File("imagesTraitées/imageTraitee.jpg"));

        System.out.println("Extraction de " + NB_COULEURS_CANDIDATES + " couleurs candidates...");
        Color[] couleursCandidates = extraction.extrairePalette(imageTraitee, NB_COULEURS_CANDIDATES, norme);

        System.out.println("Filtrage des couleurs avec un seuil de " + SEUIL_SIMILARITE + "...");
        Color[] paletteFinale = filtrerCouleursUniques(couleursCandidates, norme, SEUIL_SIMILARITE);

        Map<String, Color> paletteBiome = mapper.getBiomeMapping(paletteFinale);

        System.out.println("Couleurs candidates extraites: " + Arrays.toString(couleursCandidates));
        System.out.println("Palette finale après filtrage (" + paletteFinale.length + " couleurs): " + Arrays.toString(paletteFinale));
        System.out.println("Palette de biomes mappée: " + paletteBiome);
        System.out.println("Taille de la palette finale: " + paletteBiome.size());

        if (paletteBiome.isEmpty()) {
            System.out.println("Aucune couleur dans la palette finale, impossible de générer l'image.");
            return;
        }
        BufferedImage imgPalette = new BufferedImage(imageTraitee.getWidth(), Math.max(50, imageTraitee.getHeight() / 10), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgPalette.createGraphics();
        int startX = 0;
        int widthX = imageTraitee.getWidth() / paletteBiome.size(); // La largeur dépend du nombre de couleurs finales

        int i = 0;
        for (Map.Entry<String, Color> entry : paletteBiome.entrySet()) {
            String biomeName = entry.getKey();
            Color biomeColor = entry.getValue();

            g2d.setColor(biomeColor);
            if (i == paletteBiome.size() - 1) {
                g2d.fillRect(startX, 0, imageTraitee.getWidth() - startX, imgPalette.getHeight());
            } else {
                g2d.fillRect(startX, 0, widthX, imgPalette.getHeight());
            }

            g2d.setColor(Color.YELLOW);
            g2d.drawString(biomeName, startX + 5, 20);

            startX += widthX;
            i++;
        }
        g2d.dispose();

        String fileNamePalette = String.format("palettes/palette_dynamique_%s_%s.jpg", extraction.toString(), norme.toString());
        ImageIO.write(imgPalette, "jpg", new File(fileNamePalette));

        System.out.println("Palette sauvegardée dans: " + fileNamePalette);
    }

    /**
     * Filtre une palette de couleurs pour ne conserver que celles qui sont suffisamment distinctes.
     *
     * @param couleursEntrantes Le tableau de couleurs à filtrer.
     * @param norme La norme de couleur à utiliser pour mesurer la distance.
     * @param seuil La distance minimale (selon la norme) pour que deux couleurs soient considérées comme distinctes.
     * @return Un nouveau tableau contenant uniquement les couleurs distinctes.
     */
    private static Color[] filtrerCouleursUniques(Color[] couleursEntrantes, NormeCouleurs norme, double seuil) {
        List<Color> couleursUniques = new ArrayList<>();

        for (Color couleurCandidate : couleursEntrantes) {
            boolean estTropSimilaire = false;
            for (Color couleurDejaAjoutee : couleursUniques) {
                if (norme.distanceCouleur(couleurCandidate, couleurDejaAjoutee) < seuil) {
                    estTropSimilaire = true;
                    break;
                }
            }

            if (!estTropSimilaire) {
                couleursUniques.add(couleurCandidate);
            }
        }

        return couleursUniques.toArray(new Color[0]);
    }
}

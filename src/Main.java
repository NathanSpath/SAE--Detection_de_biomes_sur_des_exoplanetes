import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import flou.Flou;
import flou.FlouMoyenne;
import palette.BiomeMapper;
import palette.ExtractionPalette;
import palette.Palette;
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
    static final double SEUIL_SIMILARITE = 10.0;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Veuillez fournir le chemin de l'image en argument.");
            return;
        }
        File img = new File(args[0]);
        // Le constructeur de FlouMoyenne n'existe pas, j'utilise le constructeur par défaut.
        Flou methodeFlou = new FlouMoyenne(5);
        ExtractionPalette extraction = new PaletteKmeans();
        NormeCouleurs norme = new NormeBetterCIELAB();
        BiomeMapper mapper = new BiomeMapper(norme);

        // 1. Appliquer le flou
        BufferedImage imageTraitee = methodeFlou.appliquerFlou(img);
        ImageIO.write(imageTraitee, "jpg", new File("imagesTraitées/imageTraitee.jpg"));

        // 2. Extraire une palette de couleurs sur-échantillonnée
        System.out.println("Extraction de " + NB_COULEURS_CANDIDATES + " couleurs candidates...");
        Color[] couleursCandidates = extraction.extrairePalette(imageTraitee, NB_COULEURS_CANDIDATES, norme);

        // 3. Filtrer les couleurs pour ne garder que les plus distinctes
        System.out.println("Filtrage des couleurs avec un seuil de " + SEUIL_SIMILARITE + "...");
        Color[] couleursFiltrees = filtrerCouleursUniques(couleursCandidates, norme, SEUIL_SIMILARITE);

        // 4. Mapper les couleurs uniques aux biomes et créer notre objet Palette
        Map<String, Color> biomeMap = mapper.getBiomeMapping(couleursFiltrees);
        Palette palette = new Palette(biomeMap, norme);


        System.out.println("Couleurs candidates extraites: " + Arrays.toString(couleursCandidates));
        System.out.println("Palette finale après filtrage (" + couleursFiltrees.length + " couleurs): " + Arrays.toString(couleursFiltrees));
        System.out.println("Palette de biomes mappée: " + palette.getBiomeColors());
        System.out.println("Taille de la palette finale: " + palette.getNbBiomes());

        // 5. Création et sauvegarde de l'image de la palette
        if (palette.getNbBiomes() == 0) {
            System.out.println("Aucune couleur dans la palette finale, impossible de générer l'image.");
            return;
        }
        BufferedImage imgPalette = new BufferedImage(imageTraitee.getWidth(), Math.max(50, imageTraitee.getHeight() / 10), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgPalette.createGraphics();
        int startX = 0;
        int widthX = imageTraitee.getWidth() / palette.getNbBiomes(); // La largeur dépend du nombre de couleurs finales

        int i = 0;
        for (Map.Entry<String, Color> entry : palette.getBiomeColors().entrySet()) {
            String biomeName = entry.getKey();
            Color biomeColor = entry.getValue();

            g2d.setColor(biomeColor);
            if (i == palette.getNbBiomes() - 1) {
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

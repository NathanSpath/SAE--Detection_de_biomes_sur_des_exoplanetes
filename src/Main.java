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
import java.util.Arrays;
import java.util.Map;

public class Main {
    static final int NB_COULEUR_PALETTE = 10;

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

        Color[] couleursExtraites = extraction.extrairePalette(imageTraitee, NB_COULEUR_PALETTE, norme);

        Map<String, Color> paletteBiome = mapper.getBiomeMapping(couleursExtraites);

        System.out.println("Couleurs extraites: " + Arrays.toString(couleursExtraites));
        System.out.println("Palette de biomes mappée: " + paletteBiome);
        System.out.println("Taille de la palette finale: " + paletteBiome.size());


        BufferedImage imgPalette = new BufferedImage(imageTraitee.getWidth(), Math.max(50, imageTraitee.getHeight() / 10), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgPalette.createGraphics();
        int startX = 0;
        int widthX = imageTraitee.getWidth() / NB_COULEUR_PALETTE;

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

            // Dessiner le nom du biome sur la pallette
            g2d.setColor(Color.BLACK);
            g2d.drawString(biomeName, startX + 5, 20);

            startX += widthX;
            i++;
        }
        g2d.dispose();

        String fileNamePalette = String.format("palettes/palette_%s_%s.jpg", extraction.toString(), norme.toString());
        ImageIO.write(imgPalette, "jpg", new File(fileNamePalette));

        System.out.println("Palette sauvegardée dans: " + fileNamePalette);
    }
}

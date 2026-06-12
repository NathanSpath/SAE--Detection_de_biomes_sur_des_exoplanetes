import Norme.NormeBetterCIELAB;
import Norme.NormeCouleurs;
import Norme.NormeRedmean;
import flou.Flou;
import flou.*;
import palette.BiomeMapper;
import palette.ExtractionPalette;
import palette.Palette;
import palette.PaletteKmeans;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class Main {
    static final int NB_COULEUR_PALETTE =  10;

    public static void main(String[] args) throws IOException {
        File img = new File(args[0]);
        Flou methodeFlou = new FlouMoyenne(5);
        ExtractionPalette extraction =  new PaletteKmeans();
        NormeCouleurs norme = new NormeBetterCIELAB();
        BiomeMapper mapper = new BiomeMapper(new NormeBetterCIELAB());
        HashMap<String,Color> paletteBiome = new  HashMap();

        //teste du flou
        BufferedImage imageTraitee = methodeFlou.appliquerFlou(img);
        ImageIO.write(imageTraitee,"jpg", new File("imagesTraitées/imageTraitee.jpg"));

        //test de la creation de palette
        Color[] couleursExtraites = extraction.extrairePalette(imageTraitee, NB_COULEUR_PALETTE, norme);
        for (int i = 0; i < couleursExtraites.length; i++) {
            paletteBiome.put(mapper.getBiomeForColor(couleursExtraites[i]), couleursExtraites[i]);
        }
        Palette palette = new Palette(couleursExtraites, norme);
        System.out.println(paletteBiome);

        // Création et sauvegarde de l'image de la palette
        BufferedImage imgPalette = new BufferedImage(imageTraitee.getWidth(), Math.max(50, imageTraitee.getHeight() / 10), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgPalette.createGraphics();
        int startX = 0;
        int widthX = imageTraitee.getWidth() / NB_COULEUR_PALETTE;

        for (int i = 0; i < couleursExtraites.length; i++) {
            Color c = couleursExtraites[i];
            if (c == null) continue;

            g2d.setColor(c);
            if (i == couleursExtraites.length - 1) {
                g2d.fillRect(startX, 0, imageTraitee.getWidth() - startX, imgPalette.getHeight());
            } else {
                g2d.fillRect(startX, 0, widthX, imgPalette.getHeight());
            }

            startX += widthX;
        }
        g2d.dispose();

        String fileNamePalette = String.format("palettes/palette_%s_%s.jpg" , extraction.toString(), norme.toString());
        ImageIO.write(imgPalette, "jpg", new File(fileNamePalette));
    }
}
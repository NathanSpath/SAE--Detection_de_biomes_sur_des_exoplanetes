package flou;
import java.io.File;
import java.io.IOException;

public interface Flou {

    /**
     * Interface de methode de flou
     * @param image image à flouter
     */
    public void appliquerFlou(File image) throws IOException;
}

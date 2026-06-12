package flou;

public interface Flou {

    /**
     * Méthode pour le flou moyen
     * @param r
     */
    public void flouMoyenne(int r);

    /**
     * Méthode pour le flou Gausien
     * @param x
     * @param y
     * @param distribution écart type gausien
     */
    public void flouGausien(int x, int y,double distribution);
}

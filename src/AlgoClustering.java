public interface AlgoClustering {

    /**
     * Méthode pour appliquer un algoryhtme de clustering
     * @param donnees Tableau de 2 dimensions contenant les caractéristique
     * @return le nombre de cluster pour chaque objet
     */
    int[] cluster(double[][] donnees);
}

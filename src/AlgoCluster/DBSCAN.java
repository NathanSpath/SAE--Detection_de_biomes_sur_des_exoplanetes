package AlgoCluster;

import java.util.ArrayList;
import java.util.List;

public class DBSCAN implements AlgoClustering {

    private double eps;
    private int minPts;

    public DBSCAN(double eps, int minPts) {
        this.eps = eps;
        this.minPts = minPts;
    }

    @Override
    public int[] cluster(double[][] X) {

        return new int[X.length]; // Placeholder, à remplacer par l'implémentation réelle
    }

    public int[] DBSCAN(double[][] X, double eps, int minPts) {
        int[] labels = new int[X.length];
        boolean[] visited = new boolean[X.length];
        int C = 0;
        for (int i = 0; i < X.length-1; i++) {
            if (visited[i] == false) {
                visited[i] = true;
                List<Integer> Vn = regionQuery(X, i, eps);
                if (Vn.size() < minPts) {
                    labels[i] = 0;
                }else{
                    C++;
                    expandCluster(X, labels, visited, i, Vn, C, eps, minPts);
                }
            }

        }
        return labels;

    }

    private List<Integer> regionQuery(double[][] X,int n, double eps) {
        List<Integer> Vn = new ArrayList<>();
        for (int i = 0; i < X.length; i++) {
            if (i != n) {

                double somme = 0;
                for (int k = 0; k < X[i].length; k++) {
                    double diff = X[n][k] - X[i][k];
                    somme += diff * diff;
                }
                double distance = Math.sqrt(somme);

                if (distance <= eps) {
                    Vn.add(i);
                }
            }
        }
        return Vn;
    }

    private void expandCluster(double[][] X, int[] labels, boolean[] processed, int n, List<Integer> Vn, int C, double eps, int minPts) {

        //Le point de départ rejoint le cluster
        labels[n] = C;

        //On parcourt Vn
        int i = 0;
        while (i < Vn.size()) {

            int xi = Vn.get(i);  // le voisin courant

            //Si xi n'a pas encore été traité
            if (!processed[xi]) {
                processed[xi] = true;  // on le marque

                // On cherche les voisins de xi
                List<Integer> Vi = regionQuery(X, xi, eps);

                //Si xi est lui-même un core point → on étend Vn
                if (Vi.size() >= minPts) {
                    for (int v : Vi) {
                        if (!Vn.contains(v)) {
                            Vn.add(v);  // ← Vn grandit
                        }
                    }
                }
            }

            //Si xi n'appartient à aucun cluster → il rejoint C
            if (labels[xi] == -1 || labels[xi] == 0) {
                labels[xi] = C;
            }

            i++;
        }
    }
}

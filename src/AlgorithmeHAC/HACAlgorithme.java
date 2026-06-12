package AlgorithmeHAC;

import java.util.ArrayList;

public class HACAlgorithme {
    ArrayList<Cluster> clusters;
    double[][] matriceDistance;
    double seuil;
    public void initialiserClusters(ArrayList<Point> points) {
        this.clusters = new ArrayList<Cluster>();
        for (int i = 0; i < points.size(); i++) {
            Cluster cluster = new Cluster(i, points.get(i));
            this.clusters.add(cluster);
        }
    }
    public double distanceMinimaleMatrice() {
        double minDistance = Double.MAX_VALUE;
        // Parcours de la matrice pour trouver la distance minimale
        for (int i = 0; i < this.matriceDistance.length; i++) {
            for (int j = i + 1; j < this.matriceDistance.length; j++) {
                if (this.matriceDistance[i][j] < minDistance) {
                    minDistance = this.matriceDistance[i][j];
                }
            }
        }
        return minDistance;
    }

    public void executerHAC() {
        CalculDistance calculDistance = new CalculDistance();

        while (this.clusters.size() > 1) {
            // Construction de la matrice de distance entre les clusters
            int n = this.clusters.size();
            this.matriceDistance = new double[n][n];
            
            // Calcul des distances entre tous les clusters
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    this.matriceDistance[i][j] = calculDistance.distanceCluster(this.clusters.get(i), this.clusters.get(j));
                }
            }
            
            // Recherche de la distance minimale dans la matrice
            double minDistance = Double.MAX_VALUE;
            int cluster1 = -1;
            int cluster2 = -1;
            
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (this.matriceDistance[i][j] < minDistance) {
                        minDistance = this.matriceDistance[i][j];
                        cluster1 = i;
                        cluster2 = j;
                    }
                }
            }
            // Si la distance minimale est supérieure au seuil, on arrête la fusion
            if (minDistance > this.seuil) {
                break;
            }
            // Fusion des deux clusters les plus proches
            Cluster c1 = this.clusters.get(cluster1);
            Cluster c2 = this.clusters.get(cluster2);

            for (Point p : c2.points) {
                c1.ajouterPoint(p);
            }
            // Suppression du cluster fusionné
            this.clusters.remove(cluster2);
        }
    }    
}

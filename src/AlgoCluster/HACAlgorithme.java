package AlgoCluster;


import java.util.ArrayList;

public class HACAlgorithme implements AlgoClustering {

    public ArrayList<Cluster> clusters;
    public double[][] matriceDistance;
    public double seuil;

    public HACAlgorithme(double seuil) {
        this.seuil = seuil;
    }
    // Initialisation des clusters avec les points individuels
    public void initialiserClusters(ArrayList<Point> points) {
        this.clusters = new ArrayList<Cluster>();
        for (int i = 0; i < points.size(); i++) {
            Cluster cluster = new Cluster(i, points.get(i));
            this.clusters.add(cluster);
        }
    }
    // Exécution de l'algorithme HAC
    @Override
    public int[] cluster(double[][] donnees) {
        ArrayList<Point> listePoints = new ArrayList<Point>();
        for (int i = 0; i < donnees.length; i++) {
            int x = (int) donnees[i][0];
            int y = (int) donnees[i][1];
            listePoints.add(new Point(x, y));
        }

        this.initialiserClusters(listePoints);

        this.executerHAC();

        int[] labels = new int[donnees.length];
        
        int idClusterCourant = 1; 
        // Attribution des labels aux points en fonction de leur cluster
        for (Cluster c : this.clusters) {
            for (Point p : c.points) {
                int indexOrigine = listePoints.indexOf(p);
                if (indexOrigine != -1) {
                    labels[indexOrigine] = idClusterCourant;
                }
            }
            idClusterCourant++;
        }

        return labels;
    }

    public void executerHAC() {
        CalculDistance calculDistance = new CalculDistance();
        // Tant qu'il y a plus d'un cluster, on fusionne les deux clusters les plus proches
        while (this.clusters.size() > 1) {
            int n = this.clusters.size();
            this.matriceDistance = new double[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    this.matriceDistance[i][j] = calculDistance.distanceCluster(this.clusters.get(i), this.clusters.get(j));
                }
            }

            double minDistance = Double.MAX_VALUE;
            int cluster1 = -1;
            int cluster2 = -1;
            // Recherche des deux clusters les plus proches
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
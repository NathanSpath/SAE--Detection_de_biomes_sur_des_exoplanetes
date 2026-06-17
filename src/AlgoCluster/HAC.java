package AlgoCluster;

import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation de l'algorithme de Classification Ascendante Hiérarchique (CAH),
 * ou Hierarchical Agglomerative Clustering (HAC).
 * Cette version utilise la méthode de liaison moyenne (Average Linkage).
 */
public class HAC implements AlgoClustering {

    private final int k; // Nombre de clusters (écosystèmes) à trouver

    public HAC(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("Le nombre de clusters (k) doit être positif.");
        }
        this.k = k;
    }

    public int getK() {
        return k;
    }

    @Override
    public String toString() {
        return "HAC (k=" + k + ")";
    }

    @Override
    public int[] cluster(double[][] data) {
        if (data == null || data.length == 0) {
            return new int[0];
        }

        // 1. Initialisation : chaque point est son propre cluster.
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            clusters.add(new Cluster(data[i], i));
        }

        // 2. Fusions successives jusqu'à atteindre k clusters.
        while (clusters.size() > k) {
            if (clusters.size() % 100 == 0) { // Log pour suivre la progression sur de grands ensembles
                 System.out.println("HAC - Clusters restants : " + clusters.size());
            }

            double minDistance = Double.MAX_VALUE;
            int mergeIndexA = -1;
            int mergeIndexB = -1;

            // Trouver la paire de clusters la plus proche
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double distance = calculateClusterDistance(clusters.get(i), clusters.get(j));
                    if (distance < minDistance) {
                        minDistance = distance;
                        mergeIndexA = i;
                        mergeIndexB = j;
                    }
                }
            }

            // Si aucune paire n'a pu être trouvée (cas improbable), on arrête.
            if (mergeIndexB == -1) {
                break;
            }

            // Fusionner les deux clusters les plus proches
            clusters.get(mergeIndexA).merge(clusters.get(mergeIndexB));
            clusters.remove(mergeIndexB); // Important de supprimer l'index le plus élevé en premier
        }

        // 3. Attribution des labels finaux
        int[] labels = new int[data.length];
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            for (int originalIndex : cluster.getOriginalIndices()) {
                labels[originalIndex] = i + 1; // Labels commencent à 1 (0 pour le bruit dans DBSCAN)
            }
        }

        return labels;
    }

    /**
     * Calcule la distance entre deux clusters en utilisant la méthode "Average Linkage".
     */
    private double calculateClusterDistance(Cluster c1, Cluster c2) {
        double totalDistance = 0.0;
        int pairCount = 0;

        for (double[] p1 : c1.getPoints()) {
            for (double[] p2 : c2.getPoints()) {
                totalDistance += euclideanDistance(p1, p2);
                pairCount++;
            }
        }
        return pairCount == 0 ? Double.MAX_VALUE : totalDistance / pairCount;
    }

    /**
     * Calcule la distance euclidienne entre deux points.
     */
    private double euclideanDistance(double[] p1, double[] p2) {
        double sum = 0.0;
        for (int i = 0; i < p1.length; i++) {
            sum += Math.pow(p1[i] - p2[i], 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * Classe interne pour représenter un cluster.
     */
    private static class Cluster {
        private final List<double[]> points;
        private final List<Integer> originalIndices;

        Cluster(double[] initialPoint, int initialIndex) {
            this.points = new ArrayList<>();
            this.originalIndices = new ArrayList<>();
            this.points.add(initialPoint);
            this.originalIndices.add(initialIndex);
        }

        void merge(Cluster other) {
            this.points.addAll(other.points);
            this.originalIndices.addAll(other.originalIndices);
        }

        List<double[]> getPoints() {
            return points;
        }

        List<Integer> getOriginalIndices() {
            return originalIndices;
        }
    }
}

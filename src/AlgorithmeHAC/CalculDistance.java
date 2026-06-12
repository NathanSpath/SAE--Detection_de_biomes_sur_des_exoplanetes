package AlgorithmeHAC;
public class CalculDistance {
    public double distancePoint(Point p1, Point p2) {
        // Calcule de la distance euclidienne
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
    public double distanceCluster(Cluster c1, Cluster c2) {
        double minDistance = Double.MAX_VALUE;
        // Distance minimale entre tous les points des deux clusters
        for (Point p1 : c1.points) {
            for (Point p2 : c2.points) {
                double distance = distancePoint(p1, p2);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
        }
        return minDistance;
    }
}

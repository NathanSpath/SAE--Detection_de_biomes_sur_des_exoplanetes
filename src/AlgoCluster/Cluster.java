package AlgoCluster;

import java.util.ArrayList;

public class Cluster {
    int id;
    ArrayList<Point> points;
    public Cluster(int id, Point p) {
        this.id = id;
        this.points = new ArrayList<Point>();
        this.points.add(p);
    }
    public void ajouterPoint(Point p) {
        this.points.add(p);
    }
}

package AlgorithmeHAC;

import java.util.ArrayList;

public class MainTest {
    public static void main(String[] args) {
        ArrayList<Point> pointsDeTest = new ArrayList<Point>();
        
        pointsDeTest.add(new Point(1, 1));
        pointsDeTest.add(new Point(1, 2));
        pointsDeTest.add(new Point(2, 1));
        
        pointsDeTest.add(new Point(10, 10));
        pointsDeTest.add(new Point(10, 11));
        pointsDeTest.add(new Point(11, 10));

        HACAlgorithme algorithme = new HACAlgorithme();
        
        algorithme.seuil = 3.0; 
        
        algorithme.initialiserClusters(pointsDeTest);

        System.out.println("--- AVANT L'ALGORITHME HAC ---");
        System.out.println("Nombre de clusters au départ : " + algorithme.clusters.size());

        algorithme.executerHAC();

        System.out.println("\n--- APRÈS L'ALGORITHME HAC ---");
        System.out.println("Nombre de clusters finaux (écosystèmes) : " + algorithme.clusters.size());

        for (Cluster c : algorithme.clusters) {
            System.out.print("Écosystème ID " + c.id + " contient les points : ");
            for (Point p : c.points) {
                System.out.print("(" + p.x + ", " + p.y + ") ");
            }
            System.out.println();
        }
    }
}
# SAE : Détection de Biomes sur des Exoplanètes

Ce projet a pour but de développer une application capable d'analyser des images d'exoplanètes, d'en extraire les biomes principaux et de cartographier les écosystèmes qui les composent.

**Projet réalisé par :**
- MARCHAL Enzo
- CABOT Mathieu
- VINCENT Julien
- JACQUET Arthur
- SPATH Nathan

## Répartition des Tâches

- **Arthur JACQUET**:
  - Algorithme **K-Means** pour l'extraction de la palette de couleurs.
  - Développement du **`MainRecherche`** pour les tests de performance et d'optimisation.
  - Implémentation du **Flou Moyen**.
  - Conception des classes `Palette`, `BiomeMapper` et des interfaces de `Norme`.

- **Enzo MARCHAL**:
  - Implémentation de l'algorithme **DBSCAN** pour la détection des écosystèmes.
  - Contribution majeure à l'intégration et à l'optimisation des algorithmes dans les `Main` de test.

- **Nathan SPATH**:
  - Implémentation du **Flou Gaussien**.
  - Aide à la recherche et à l'implémentation dans les `Main` de test, notamment `MainDBSCAN`.

- **Julien VINCENT**:
  - Implémentation de l'algorithme **HAC (Hierarchical Agglomerative Clustering)**.
  - Aide à l'intégration et aux tests.

- **Mathieu CABOT**:
  - Coordination générale, aide sur tous les fronts.
  - **Rédaction du rapport** et de la documentation.
  - Assistance à Julien et Nathan.

## Présentation de l'Application

L'application suit un pipeline de traitement en plusieurs étapes pour analyser une image d'exoplanète :
1.  **Pré-traitement** : Une technique de **flou gaussien** est appliquée pour lisser l'image et réduire le bruit, ce qui améliore la cohérence des zones de couleur.
2.  **Extraction de Palette** : L'algorithme **K-Means** analyse l'image pour en extraire les couleurs les plus représentatives, formant une palette de base.
3.  **Mappage des Biomes** : La classe `BiomeMapper` associe les couleurs de la palette générée à une liste prédéfinie de biomes, créant ainsi une carte des biomes de la planète.
4.  **Détection des Écosystèmes** : Pour chaque biome identifié, l'algorithme **DBSCAN** est utilisé pour regrouper les pixels en clusters spatiaux, représentant les différents écosystèmes au sein d'un même biome.

Une attention particulière a été portée à l'optimisation des performances, avec une gestion multi-threadée et des stratégies de clustering adaptatives pour garantir une exécution rapide (entre 5 et 10 minutes).

## Structure du Projet

Le projet est organisé autour de plusieurs modules clés :

- **`src/`**: Contient l'ensemble du code source de l'application.
  - **`Main.java`**: Le point d'entrée principal, qui orchestre l'ensemble du pipeline.
  - **`AlgoCluster/`**: Contient les implémentations des algorithmes de clustering (`DBSCAN`, `HAC`).
  - **`flou/`**: Contient les différentes implémentations des algorithmes de flou (`FlouMoyenne`, `FlouGausien`).
  - **`palette/`**: Classes responsables de la gestion des couleurs et des biomes (`Palette`, `PaletteKmeans`, `BiomeMapper`).
  - **`Norme/`**: Interfaces et classes pour le calcul de la distance entre les couleurs.
- **`Images/`**: Contient les images d'exoplanètes à analyser.
- **`resultats_analyse/`**: Le répertoire de sortie où les images générées (cartes de biomes, écosystèmes) sont sauvegardées.

## Développement et Composants Clés

- **Interfaces (`ExtractionPalette`, `Flou`, `Norme`)**: Assurent une architecture modulaire et extensible.
- **Algorithmes de Clustering (`PaletteKmeans`, `DBSCAN`)**: Le cœur de l'analyse, K-Means pour les couleurs et DBSCAN pour l'espace.
- **`BiomeMapper`**: Fait le lien intelligent entre les couleurs extraites et les noms de biomes, en garantissant des associations uniques et cohérentes.
- **Optimisation des Performances**: Le `Main.java` final inclut une stratégie de clustering à trois niveaux (Normal, Rapide, Très Rapide) pour traiter efficacement les biomes de différentes tailles, garantissant ainsi des temps d'exécution maîtrisés.
- **Paramétrage Facile**: Une classe `Config` centralise tous les paramètres ajustables, permettant de modifier facilement le comportement de l'application pour des tests ou des optimisations.

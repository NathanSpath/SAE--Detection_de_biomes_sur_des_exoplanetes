# Flux de Travail pour la Détection des Biomes et Écosystèmes

Ce document décrit la stratégie proposée pour identifier les biomes sur une carte d'exoplanète, puis pour détecter les écosystèmes individuels au sein de chaque biome.

## Partie 1 : Détection et Visualisation des Biomes

La première étape consiste à créer une carte des biomes simplifiée basée sur une palette de couleurs limitée.

1.  **Génération de la Palette** :
    *   Utiliser un algorithme de clustering (par exemple, K-Means) sur l'image source pour extraire une palette de couleurs représentative. Chaque couleur de cette palette correspond à un biome principal.

2.  **Génération de la Carte des Biomes** :
    *   Créer la carte principale des biomes en parcourant chaque pixel de l'image originale.
    *   Pour chaque pixel, calculer sa distance de couleur par rapport à chaque couleur de la palette générée.
    *   Attribuer au pixel la couleur du biome le plus proche dans la palette.
    *   Ce processus génère une carte propre et simplifiée de tous les biomes sans utiliser de clustering spatial complexe sur l'image entière.

3.  **Visualisation de Biome Individuel (Fonctionnalité Optionnelle)** :
    *   Implémenter une fonctionnalité pour générer une image qui ne met en évidence que les pixels appartenant à un seul biome sélectionné par l'utilisateur. Ceci est utile pour l'analyse et le débogage.

## Partie 2 : Détection des Écosystèmes

La deuxième étape consiste à identifier les écosystèmes spatialement séparés au sein de chaque biome. Deux régions déconnectées du même biome sont considérées comme deux écosystèmes distincts.

1.  **Préparation des Données (Regroupement par Biome)** :
    *   Après avoir généré la carte des biomes, parcourir l'image une fois de plus.
    *   Créer des sous-listes de coordonnées de pixels, en les regroupant par le biome qui leur a été attribué. Le résultat sera une structure de données de type `Map<Biome, List<Point>>`, où chaque liste contient toutes les coordonnées `(x, y)` des pixels d'un biome spécifique.

2.  **Clustering des Écosystèmes** :
    *   Appliquer un algorithme de clustering spatial (par exemple, **DBSCAN** ou **Classification Ascendante Hiérarchique - CAH**) à chacune de ces sous-listes spécifiques à un biome.
    *   Le clustering sera effectué uniquement sur les coordonnées `(x, y)` pour identifier les groupes spatialement connectés.
    *   **Avantage** : En exécutant l'algorithme sur des listes plus petites et par biome, nous pouvons traiter un nombre beaucoup plus grand de pixels et atteindre une précision plus élevée qu'en l'exécutant sur l'image entière en une seule fois.

3.  **Visualisation des Écosystèmes** :
    *   Pour un biome sélectionné, générer une visualisation de ses écosystèmes.
    *   Attribuer une couleur unique et aléatoire à chaque écosystème (c'est-à-dire à chaque cluster trouvé par DBSCAN/CAH).
    *   Cela montrera clairement toutes les régions indépendantes et non contiguës d'un même biome. Par exemple, si le biome "Forêt" existe sur deux continents distincts, ils seront rendus dans deux couleurs différentes, les identifiant comme deux écosystèmes distincts.

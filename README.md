# SAE Detection de biomes sur des exoplanetes
Projet realisé par:
- MARCHAL Enzo
- CABOT Mathieu
- VINCENT Julien
- JACQUET Arthur
- SPATH Nathan

## repartition des taches

## Presentation de l'application

## Structure du projet

## Ce qu'on a realisé

## Développement et Composants Clés

Cette section détaille les composants développés pour l'analyse d'images et la détection de biomes.

### Interfaces et Normes

- **`ExtractionPalette`**: Une interface définissant la stratégie pour extraire une palette de couleurs à partir d'une image.
- **`Flou`**: Une interface pour l'application de différents algorithmes de flou sur une image.
- **`Norme`**: Une interface pour le calcul de la distance entre les couleurs, essentielle pour le clustering et le mappage.
- **`NormeRedmean`**, **`NormeBetterCIELAB`**: Des implémentations concrètes de l'interface `Norme`, offrant différentes méthodes de calcul de distance colorimétrique.

### Algorithmes de Traitement d'Image

- **`FlouMoyenne`**: Une implémentation de `Flou` qui applique un flou en moyennant la couleur des pixels voisins.
- **`FlouGausien`**: Une implémentation plus avancée qui utilise un noyau gaussien pour un flou plus doux et naturel.

### Extraction et Gestion de Palette

- **`PaletteKmeans`**: L'implémentation principale de `ExtractionPalette`. Elle utilise l'algorithme de clustering K-Means pour identifier les `N` couleurs les plus représentatives d'une image. Pour des raisons de performance, l'algorithme travaille sur une version redimensionnée de l'image.
- **`BiomeMapper`**: Une classe cruciale qui fait le lien entre les couleurs extraites et les biomes connus. Elle prend une palette de couleurs générée et l'associe à une palette de référence (couleur -> nom de biome) en trouvant la meilleure correspondance unique pour chaque couleur, garantissant ainsi qu'aucun biome n'est attribué plus d'une fois.
- **`Palette`**: Une classe utilitaire pour la gestion et la manipulation des palettes de couleurs.


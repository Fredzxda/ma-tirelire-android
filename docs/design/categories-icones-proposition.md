# Icônes de catégories — proposition du 22 novembre 2026

Statut : placement validé par Frédéric le 22 novembre 2026 : icônes avant le
statut, au plus trois pictogrammes distincts puis +N pour une ventilation.
L’intégration Android reste au prochain lot. Aucun changement de
coffre ou de format de sauvegarde n’est effectué par cette maquette.

Ouvrir [le catalogue interactif](categories-icones-proposition.html). Il contient
64 pictogrammes originaux, répartis en huit familles de huit. Le catalogue se
filtre par nom ou famille. Cliquer sur les pictogrammes modifie l’aperçu d’une
opération simple ou ventilée. Le choix de position avant/après le statut et
l’affichage des icônes peuvent être simulés. Tout reste en mémoire du navigateur.

## Proposition de placement

- En tête de carte, icône à gauche du statut, puis date alignée à droite.
- Hors série, même place, sans inventer de statut.
- Ventilation : au plus trois icônes distinctes, puis +N ; pas de doublon si
  plusieurs lignes emploient la même icône. Les catégories restent lisibles sous
  la description, sans préfixe « Ventilation : ».
- Les icônes ne remplacent ni les noms, ni la confirmation, ni le pointage.
- Les virements internes affichent une seule mention, avec la typographie
  habituelle des métadonnées.

## Catalogue

Le [JSON versionné](categories-icones-catalogue.json) fournit les 64 identifiants,
noms, familles et tracés SVG. Grille 24 × 24, contours monochromes de 1,7,
extrémités et jonctions arrondies. Dessins géométriques préparés pour cette
proposition ; aucune provenance Material, Lucide ou autre n’est revendiquée.
Les futurs VectorDrawable peuvent reprendre ces géométries. Les détails seront
à vérifier sur écran Android à 20–24 dp avant la livraison des icônes réelles.

## Intégration Android proposée ensuite

1. Ajouter un choix d’icône facultatif aux propriétés de catégorie, depuis les
   réglages et la création de catégorie ; grille filtrable et option Aucune.
2. Persister uniquement un identifiant stable, dans le coffre chiffré et les
   sauvegardes. Les anciens formats ont par défaut aucune icône. Aucun nom de
   catégorie n’est modifié et aucun pictogramme n’est affecté automatiquement.
3. Héritage du parent proposé en option explicite ; distinguer « Aucune » et
   « Utiliser celle du parent » afin de pouvoir réellement désactiver une icône.
4. Préférence globale pour masquer les pictogrammes sur les cartes. Le choix
   d’une catégorie conserve son nom à côté du pictogramme et reste utilisable
   au lecteur d’écran ; le dessin n’est pas l’unique porteur d’information.
5. Vérifier portrait, paysage, grands textes et catégories longues, statut
   absent/présent, ventilation avec 1 à plus de 3 icônes et thème sombre.

## Limites de la proposition

Les libellés de la maquette sont français ; l’intégration devra fournir FR/EN.
Cette page n’implémente pas encore de sélecteur dans l’application, de persistance
des affectations, ni de migration. Son aperçu n’est pas une capture de l’APK.

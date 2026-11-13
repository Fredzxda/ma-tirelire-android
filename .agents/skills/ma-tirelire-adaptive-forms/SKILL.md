---
name: ma-tirelire-adaptive-forms
description: Concevoir ou modifier les formulaires et sélecteurs de Ma Tirelire Android en conservant le portrait validé et un défilement utilisable en paysage, clavier ouvert et texte agrandi.
---

# Formulaires Ma Tirelire : portrait et paysage

Appliquer aux fiches de saisie, réglages et sélecteurs du projet
`C:/Dev/ma-tirelire-android`. Pour la liste Opérations, conserver aussi le contrat
`.agents/skills/ma-tirelire-operations-ui/SKILL.md`.

## Portrait validé

- Garder une colonne principale lisible ; ne pas comprimer les champs dans
  plusieurs colonnes pour gagner artificiellement de la hauteur.
- Dans les réglages de catégories, arbre au-dessus des propriétés sur téléphone,
  avec défilements distincts et hauteur utile pour les deux. Sur tablette large,
  deux panneaux sont possibles. L'en-tête ne doit pas évincer le contenu.
- Libellés visibles dans l'encoche des champs même vides. Même typographie et
  hauteur de base pour les contrôles voisins ; texte long autorisé à revenir
  à la ligne. Ne pas remplacer les sélecteurs de date par du texte libre.
- Conserver le point décoratif des catégories sans enfant et les flèches sur
  celles avec enfants. Flèche et nom restent deux actions tactiles distinctes.

## Paysage et hauteur disponible

- Ne pas déduire une disposition utilisable uniquement d'un seuil de largeur.
  Tenir compte de l'orientation, de la hauteur, du clavier et des grands textes.
- Lorsque titre, onglets et boutons ne laissent qu'une ligne de liste, déplacer
  les commandes dans un rail latéral défilant. Arbre et formulaire disposent
  alors de toute la hauteur et défilent indépendamment.
- Un sélecteur avec recherche garde une véritable zone de résultats défilante ;
  les commandes Fermer/Créer/Aucune ne doivent pas occuper tout le bas de l'écran.
- Tous les champs et actions de fin de formulaire doivent rester atteignables.
  Des cibles de 48 dp et des caractères lisibles priment sur le « tout sans scroll ».

## Vérifications concrètes

- Sur données fictives : portrait puis paysage, au moins 40 lignes, noms longs,
  plusieurs niveaux, clavier ouvert/fermé, texte Android agrandi.
- Atteindre la dernière ligne de l'arbre ET la dernière action des propriétés ;
  sélectionner une ligne, rechercher un enfant et effacer la recherche.
- Vérifier la conservation à la rotation. Garder les libellés, recherches et
  brouillons sensibles dans le ViewModel ou le coffre chiffré ; pas dans un
  Bundle Android/rememberSaveable. Les clés de listes sauvegardables ne doivent
  pas être des libellés financiers.
- Réutiliser le harnais Play. Le test
  `landscapeCategoryPanesKeepHeightAndScrollToLastItems` démontre la hauteur et
  le défilement des panneaux ; l'étendre si leur organisation change. Un succès
  de compilation ne vaut pas observation sur téléphone. Signaler les variantes
  non essayées et ne pas déclarer un test exécuté lorsqu'ADB est absent.

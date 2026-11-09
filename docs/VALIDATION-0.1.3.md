# Validation locale — 0.1.3-dev

9 novembre 2026. Branche `dev/landscape-0.1.3`, basée sur `45fdd4f`.

## Organisation de la saisie

À partir de 600 dp disponibles, comptes uniquement à gauche, empilés dans une
colonne de 144 dp. Toute la saisie se trouve à droite : sens de l'opération,
puis montant/description côte à côte, puis catégorie/moyen côte à côte.
La largeur maximale de la surface passe de 960 à 1200 dp.
Les boutons de création de référence reviennent à la ligne si nécessaire.
En fenêtre étroite, la présentation en une colonne reste utilisée.

Objectif : rendre les champs usuels visibles sans défilement sur téléphone en
paysage, clavier fermé. Le défilement reste disponible si le clavier, les grands
caractères ou les champs de création supplémentaires réduisent l'espace.
Aucune modification des règles comptables ni du stockage du brouillon.

## Contrôles locaux

Compilation APK, sept tests existants et lint réussis via :
`assembleDebug testDebugUnitTest lintDebug :app:printAppVersion`.
Inspection de la version APK et BuildConfig : `0.1.3-dev`, versionCode 4.
Index contrôlé avant commit. Le fichier local non suivi
`gradle/gradle-daemon-jvm.properties` créé par l'environnement est préservé et
exclu du commit de cette correction.

Pas de validation visuelle sur appareil ni d'installation dans cette étape.
Les tests du modèle ne prouvent pas l'absence de défilement sur un écran réel.

## Essais sur téléphone

1. En paysage : comptes verticalement à gauche, saisie entièrement à droite.
2. Clavier fermé : vérifier l'accès aux quatre champs sans défilement.
3. Clavier ouvert : vérifier champ actif et boutons Annuler/Enregistrer.
4. Créer une catégorie ou un moyen : boutons accessibles, défilement si nécessaire.
5. Rotation portrait/paysage : brouillon conservé et une seule opération enregistrée.

Toujours une démonstration en mémoire ; ne pas saisir de données réelles.
Retour arrière : commit `45fdd4f`, sans migration de base persistante.

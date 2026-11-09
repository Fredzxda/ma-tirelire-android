# Validation locale — 0.1.2-dev

9 novembre 2026. Branche `dev/landscape-0.1.2`, issue de `8783cdc`.

## Correction

Le formulaire utilisait un AlertDialog de largeur standard, peu adapté à la faible
hauteur disponible en paysage. Il utilise désormais un Dialog dont la surface
occupe l'espace disponible (largeur maximale 960 dp), avec prise en compte des
insets système et clavier par `WindowInsets.safeDrawing`.

- Une colonne dans une fenêtre étroite ; deux colonnes dès 600 dp disponibles.
- En deux colonnes : compte, sens, montant et description à gauche ; catégories
  et moyens à droite. Chaque colonne défile indépendamment.
- En une colonne : tous les champs sont défilables.
- Annuler et Enregistrer restent dans le bandeau supérieur, hors du défilement.
- Le brouillon reste dans le même ViewModel ; aucun changement de calcul,
  de persistance ou de règles de pointage.
- SecureOn reste appliqué à la fenêtre de saisie.

Référence pour les insets :
https://developer.android.com/develop/ui/compose/system/insets-ui

## Contrôles

Commande locale : `assembleDebug testDebugUnitTest lintDebug :app:printAppVersion`.
Compilation, sept tests existants et lint réussis. Version générée et APK inspectés :
`0.1.2-dev`, versionCode 3, minimum API 31, aucune permission INTERNET.
Les tests du modèle ne constituent pas un test visuel du redimensionnement.

## Vérification sur téléphone

1. Vérifier le numéro 0.1.2-dev affiché.
2. Ouvrir la saisie en portrait, saisir montant/description puis tourner en paysage :
   formulaire et brouillon conservés, disposition sur deux colonnes si largeur suffisante.
3. Ouvrir le clavier dans chaque colonne, notamment en créant un moyen ou une catégorie :
   champ actif accessible par défilement et boutons Annuler/Enregistrer accessibles.
4. Revenir en portrait, enregistrer : une seule opération et solde correct.
5. Refaire avec grande taille de police et sur le second téléphone si possible.

Pas d'installation, ni d'essai visuel sur appareil ou émulateur dans cette étape.
Toujours une démonstration sans stockage persistant, biométrie ni coffre chiffré.
Ne pas saisir de données réelles. Retour arrière possible au commit `8783cdc` ;
aucune base persistante n'est à migrer.

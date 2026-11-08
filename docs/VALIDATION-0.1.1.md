# Validation locale — 0.1.1-dev

8 novembre 2026. Branche `dev/interface-0.1.1`, issue de `dev/initial-android`
au commit `66756b4`. Aucune publication distante ni installation sur appareil.

## Changements

- Total et pointé du compte affichés ensemble dans la liste des comptes et celle
  des opérations ; consolidation sur l'accueil. Solde initial inclus dans les deux.
- Catégories et moyens de paiement choisis dans des listes réutilisables en mémoire.
  Action Ajouter dans chaque liste, puis sélection de l'entrée créée. Nom vide
  refusé ; nom identique après trim, sans tenir compte de la casse, réutilisé.
- Le brouillon entier et l'affichage du formulaire sont conservés dans le
  ViewModel : montant, compte, signe, description, sélections et création en cours.
  Rotation et pause ne suppriment plus ce modèle. Annuler abandonne l'opération ;
  une référence déjà explicitement créée reste disponible dans la session.
- REC-001 documente les opérations périodiques pour un lot ultérieur.

## Vérifications réalisées

- Wrapper Gradle : `assembleDebug testDebugUnitTest lintDebug :app:printAppVersion`
  réussi. Sept tests, aucun échec : deux monétaires et cinq du modèle de démo.
- Tests : total/pointé avec solde initial, séparation des comptes, pointer/dépointer,
  création et réutilisation des références, absence de doublon de nom ou d'opération,
  conservation des champs du brouillon dans le modèle, annulation et saisie invalide.
- Clés FR/EN identiques et documentation lue en UTF-8 sans caractère de remplacement.
- APK inspecté avec aapt : `org.matirelire.android.demo`, versionCode 2,
  versionName `0.1.1-dev`, minimum API 31, cible 37 ; aucune permission INTERNET.
- Version du BuildConfig utilisé à l'écran : `0.1.1-dev`.

## Essais à faire sur téléphone

1. Vérifier la version 0.1.1-dev affichée.
2. Sur les données initiales, compte courant : total 2 849,70 EUR, pointé
   2 853,20 EUR. Pointer le café de -3,50 : total inchangé, pointé 2 849,70 EUR.
   Dépointer rétablit le pointé précédent.
3. Saisir une opération, remplir tous les champs puis tourner l'écran : formulaire
   et contenu présents. Refaire en cours de création d'une catégorie/moyen.
4. Passer en arrière-plan puis revenir : pause ; Reprendre retrouve le brouillon.
5. Créer catégorie et moyen, enregistrer puis ouvrir une nouvelle saisie : entrées
   disponibles. Essayer le même nom avec casse/espaces différents : pas de doublon.
6. Annuler une saisie : aucune écriture ni modification de solde. Enregistrer :
   une seule ligne, non pointée ; seul le total change avant son pointage.
7. Vérifier FR/EN, portrait/paysage et accès aux boutons avec le clavier ouvert.

## Limites

Tests du modèle sans observation physique de la rotation ou de l'interface dans
cette étape. Les essais précédents de l'utilisateur portaient sur 0.1.0-dev.
CI non exécutée sur GitHub. Les menus déroulants eux-mêmes peuvent se refermer à
la rotation, mais le formulaire et ses données doivent rester disponibles.
Référentiels plats : hiérarchie, filtrage par compte/débit/crédit, édition et
suppression des références restent à livrer dans leurs lots. Libellés d'exemples
initialisés conservés dans leur langue pendant la session.
Tout reste en mémoire, perdu si le processus est arrêté. Pas de chiffrement,
biométrie, sauvegarde/restauration ni opérations périodiques implémentés : aucune
donnée réelle. Le masque de pause n'est pas une authentification.

Retour arrière : le commit précédent reste compilable ; la démo n'a pas de base
persistante à migrer. Un retour de version ne préserve pas la session en mémoire.

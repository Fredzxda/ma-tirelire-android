# Validation 0.7.12-dev — état au 15 novembre 2026

- Compilations Debug et APK de tests, lint : réussis.
- 162 tests unitaires, zéro échec : réussis sur la source de cette livraison.
- Cas couverts : montant courant du modèle (et non première occurrence), sens
  débit/crédit, devises différentes, activité conservée, pause volontaire conservée,
  curseur sans duplication, archivage puis suppression, désarchivages successifs,
  anciens modèles internes suspendus convertis sans réactivation implicite.
- Revue indépendante statique : aucune régression bloquante identifiée dans la
  conversion future, le remappage et la migration des anciennes archives.
- Le test instrumenté des glissements est étendu au bouton Désarchiver direct.
  APK de tests compilé ; exécution de cette version sur ASUS en attente de connexion.
- APK produit : versionName 0.7.12-dev, versionCode 42.

Le 15 novembre, ADB ne voit aucun appareil : installation et relancement non
effectués pour la 0.7.12-dev. La réponse de Frédéric confirmant la reconnexion
précédente a bien été reçue ; une nouvelle connexion actuelle est nécessaire.
Les succès des tests ASUS de la 0.7.11 ne valent pas validation de la 0.7.12.

Essais et comportement : [Comptes et séries externes](gestion-comptes-0.7.12.md).
Le rendu des couleurs, des icônes et des coins doit encore être confirmé visuellement.

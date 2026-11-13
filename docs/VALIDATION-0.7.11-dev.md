# Validation 0.7.11-dev — 13 novembre 2026

- Lot : archivage/suppression de comptes, confirmation après glissement,
  désarchivage en configuration. Suppression de l’historique autorisée avec
  conservation des contreparties et des soldes des autres comptes.
- Compilations Debug et APK de tests, lint : réussis.
- 157 tests unitaires : réussis. Neuf tests de cycle de vie des comptes couvrent
  archivage, suspension, lecture seule, suppression, conservation des contreparties,
  états prévisionnels, réindexage, format d’archive et ancienne occurrence dont
  le modèle épuisé vise un compte archivé.
- 12 tests ASUS : réussis ; gestes gauche/droite, annulation, absence de mutation
  avant confirmation, vérifications de stockage et d’interface sur données fictives.
- Harnais : installation 0.7.11-dev vérifiée et application relancée.
- Revue indépendante statique : une reprise indirecte de série archivée a été
  identifiée, corrigée et couverte par un test avant livraison.

Limites : pas de suppression ni de restauration de comptes personnels pour les
tests. Le dernier compte est archivable mais non supprimable. La vérification
visuelle utilisateur et les autres appareils restent à faire. Scénarios et règles :
[Gestion des comptes](gestion-comptes-0.7.11.md). Aucun tag ni publication stable.

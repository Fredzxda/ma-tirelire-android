# Validation 0.7.13-dev — 16 novembre 2026

Version compilée : versionName 0.7.13-dev, versionCode 43.

Périmètre : ventilation des opérations et réduction de l’anticipation, sans
perte des écritures confirmées/pointées ni résurrection des suppressions manuelles.

## Contrôles

- Compilation de l’application et de son APK de tests réussie.
- 175 tests unitaires réussis, aucune erreur ni test ignoré.
- Lint réussi. Avertissement existant de dépréciation des API Compose de tests.
- Métadonnées de l’APK vérifiées : 0.7.13-dev, versionCode 43.
- Revue indépendante des invariants financiers et de persistance effectuée.
- Tests Android ajoutés pour EntrySplits en portrait et paysage dans une activité
  fictive : défilement jusqu’aux deux montants puis enregistrement. Ils ne couvrent
  pas à eux seuls la fiche complète, la rotation en cours de saisie ou le clavier.

Installation, lancement et tests instrumentés non effectués : l’ASUS configuré
n’était pas disponible dans ADB lors des deux vérifications du 16 novembre.
Aucune validation sur téléphone n’est revendiquée à ce stade. Relancer
`powershell -NoProfile -File tools/play.ps1` après reconnexion de cet appareil.

APK local : `app/build/outputs/apk/debug/app-debug.apk`.
CI GitHub : non observée au moment de cette validation locale.

Scénarios : [Ventilation et anticipation](ventilation-et-anticipation-0.7.13.md).

Avant tout retour à une ancienne version, conserver une sauvegarde chiffrée
compatible avec celle-ci. Les nouveaux formats contenant des ventilations ou
des prévisions différées ne sont pas lisibles par la 0.7.12-dev. Ne pas effacer
le coffre personnel pour tester un retour arrière.

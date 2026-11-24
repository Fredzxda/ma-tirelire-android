# Validation et coût des échanges — 23 novembre 2026

Le temps CPU des tests locaux et le quota du modèle sont distincts. L’objectif
est de réduire les allers-retours, les sorties et le contexte répété, sans
abandonner les contrôles qui protègent les données.

## Règles

- Petit changement : un responsable, lectures ciblées. Délégation seulement
  si elle apporte un gain, avec contexte minimal et périmètre sans recouvrement.
- Pendant l’édition : compiler et exécuter les tests du module touché. Ne pas
  refaire les essais physiques après chaque modification de texte.
- Avant livraison : compilation, tests unitaires, lint et suite appareil via
  Play. Une seule passe complète tant que rien ne justifie une nouvelle passe.
- Si un test échoue : diagnostic limité à sa trace et ses fichiers. Correction
  puis test ciblé ; élargir seulement en présence d’un risque non résolu.
- Les changements de formats, de chiffrement, de soldes ou de sauvegardes
  justifient une non-régression plus large. Un libellé ou une documentation seule
  ne justifie pas de rejouer SQLCipher/Keystore.
- Ne pas multiplier les tests qui recopient l’implémentation. Couvrir les
  comportements et erreurs réellement possibles, données de test uniquement.
- Garder les journaux hors conversation ; communiquer nombres, résultat et
  limites. Conserver dans VALIDATION la portée exacte des preuves.
- Après une interruption : statut Git, dernier état de validation et résumé
  de reprise ; pas de relecture intégrale de toutes les archives.

## Commandes

Livraison complète : `powershell -NoProfile -File tools/play.ps1`.
Correction du seul test d’icônes : compiler `assembleDebugAndroidTest`, puis
`powershell -NoProfile -File tools/test-vault-device.ps1 -Suite Icons`.
Ce mode ne remplace pas la validation d’une modification du coffre.

Les tâches Gradle marquées UP-TO-DATE sont réutilisées ; ne pas faire clean
par défaut. Les quotas exacts ne sont pas estimés à partir du nombre de tests.

Calendrier de prochaine échéance : mode appareil ciblé `-Suite NextDate`
après compilation du seul APK de tests si le code applicatif est inchangé.

# Ma Tirelire Android

- Appliquer le skill dev-delivery ; toute version de developpement est suffixee -dev.
- Workflow local de livraison : `powershell -NoProfile -File tools/play.ps1`.
  Il compile, teste, controle lint, installe et ouvre la demo sur l'ASUS configure.
- Frederic a autorise le 9 novembre 2026 l'installation et le lancement sur cet
  ASUS apres chaque nouvelle compilation validee. Ne pas redemander cet accord.
- Identite physique dans `tools/device.local.json`, local et ignore de Git.
  Aucun appareil de remplacement si la cible est absente ou ambigue.
- Conserver `tools/build.ps1` pour les compilations sans appareil et la CI.
  Si l'ASUS est deconnecte, signaler que l'installation/lancement n'a pas ete fait.
- Cette autorisation ne couvre ni redemarrage du telephone, flashage, desinstallation,
  publication stable, ni installation sur un autre appareil.
- Verifier la version installee et le resultat du lancement ; ne pas confondre
  ouverture reussie et validation visuelle de l'interface.

- Play compile aussi l APK de tests et execute les controles SQLCipher/Keystore
  isoles sur ASUS avant de lancer l application. Le coffre utilisateur reste intact.

- Chaque nouvelle version doit actualiser les ressources FR/EN release_notes_changes
  et release_notes_tests : nouveautes et scenarios manuels realisables. Le dialogue
  apparait apres deverrouillage et se retrouve dans les reglages.

- Statistiques de revenus/depenses et par categorie : utiliser le perimetre
  reportingTransactions(), qui exclut les deux cotes des virements internes
  via peerId. Un virement externe sans lien reste une operation ordinaire.

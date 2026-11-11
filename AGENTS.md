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

- Edition de serie : choix au moment de sauvegarder, operation courante seule
  ou modele futur. Ne jamais propager sur les autres ecritures deja enregistrees,
  meme non confirmees. Une paire de virement reste atomique. Tester dates,
  frequence, limites et preservation de l'historique lors des changements.

- Exception au dialogue de portee : si seuls les parametres propres a la serie
  (frequence, fin, nombre restant) changent, enregistrer directement la serie,
  sans confirmer ni modifier cette ecriture ni remplacer les champs du modele.

- Anticipation : les previsions generees entrent dans le solde total mais pas
  dans les rapports revenus/depenses tant que futures et non confirmees.
  Confirmation et pointage restent independants. Reduire un horizon ne supprime
  jamais les ecritures deja enregistrees. Le reglage reste chiffre/sauvegarde.

- Statut de recurrence seul : aucun dialogue de portee. Prevision -> A confirmer
  -> Confirmee en deux actions distinctes, etat intermediaire persiste avant
  echeance. Aucun pointage automatique ; une paire change ses deux etats ensemble.

- Avant de modifier la presentation de la vue Operations, lire le skill projet
  .agents/skills/ma-tirelire-operations-ui/SKILL.md et la reference de conception
  docs/design/operations-proposition.md. La proposition du 11 novembre 2026
  a ete validee par Frederic avec autorisation explicite d implementation.

- Play execute aussi OperationsUiDeviceTest dans une activite vierge avec
  donnees fictives ; ne jamais utiliser le coffre personnel pour ces tests.

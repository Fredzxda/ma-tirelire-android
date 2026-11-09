# Ouverture SQLCipher — 0.2.4-dev

9 novembre 2026. Base : `a94253b`, branche dev/vault-0.2.0.

## Diagnostic et correction

Après correction de l'ordre AAD/authentification, la clé enveloppée était écrite
mais la base restait vide. L'exception se situait à database-open.
Le test ajouté appelle le véritable VaultStore avec un ContextWrapper pointant
uniquement vers un dossier de cache aléatoire isolé.

Avant correction, ce test échouait sur ASUS à `PRAGMA secure_delete=ON` :
« Queries can be performed using SQLiteDatabase query or rawQuery methods only ».
Les réglages PRAGMA qui peuvent renvoyer une ligne sont maintenant exécutés via
rawQuery, puis leur curseur est fermé. Aucun changement de clé ni effacement du
coffre existant.

## Validation réalisée

- Compilation, 22 tests unitaires et lint réussis.
- Trois tests instrumentés sur ASUS réussis : chiffrement SQLCipher/mauvaise clé,
  initialisation-écriture-fermeture-réouverture via le véritable VaultStore,
  refus de la clé Keystore sans authentification de son opération.
- Le nouveau test a échoué avant correction puis réussi après correction.
- Le harnais Play compile désormais aussi l'APK de tests, installe la version,
  vérifie son numéro, exécute les contrôles isolés puis lance l'application.
  Ce parcours complet a été exécuté avec succès sur ASUS.
- L'utilisateur confirme que le coffre existant s'ouvre et que les comptes
  s'affichent sur 0.2.4-dev.

Le test physique ne lit ni la clé ni la base de l'utilisateur. Il supprime seulement
son propre dossier de test après vérification de son emplacement dans le cache.

## Vérifications utilisateur restantes, avec données fictives

1. Ajouter une dépense fictive et la pointer. Passer en arrière-plan, revenir :
   authentification exigée, écriture et pointage retrouvés après ouverture.
2. Fermer complètement le processus puis rouvrir : même exigence et mêmes données.
3. Rotation pendant saisie : brouillon conservé ; absence de double opération.
4. Annuler l'authentification : aucun contenu du coffre accessible.
5. Si un code supplémentaire a été configuré : mauvais code refusé puis bon code
   accepté ; ne communiquer aucun code dans un rapport de test.
6. Dans Réglages, choisir un dossier local Documents/MaTirelire. Au retour du
   sélecteur, réauthentifier, puis configurer un mot de passe de sauvegarde distinct.
7. Sauvegarder manuellement, modifier une donnée fictive, restaurer l'archive :
   retour à l'état attendu. Mauvais mot de passe : aucune donnée remplacée.
8. Restaurer le fichier sur le Samsung avec son mot de passe : test croisé encore
   requis ; pas de désinstallation ni d'effacement du coffre ASUS pour ce test.

Captures/aperçu, échecs de stockage et rotation hebdomadaire restent à observer.
Les autres limites de VALIDATION-0.2.0.md continuent de s'appliquer. Ne pas
confier de vraies données à ce prototype avant validation de la récupération.

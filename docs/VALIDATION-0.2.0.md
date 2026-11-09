# Validation — prototype 0.2.0-dev

9 novembre 2026. Branche `dev/vault-0.2.0`, base `c541aea`.
Application conservée sous l'identifiant de développement `.demo`.

## Livré

- Coffre SQLCipher Community 4.20.0, dans noBackupFilesDir ; pas de SQLite en clair.
- Clé aléatoire 256 bits, enveloppée par AES-GCM dans Android Keystore ; clé
  non exportable dont chaque opération exige une authentification Android.
- Choix code/verrouillage du téléphone, ou biométrie forte avec secours système.
- Code supplémentaire facultatif à la création (8 caractères minimum), protégeant
  la clé par Argon2id et AES-GCM à l'intérieur de l'enveloppe matérielle.
- Session fermée et modèle vidé au véritable arrière-plan ; rotation conservée.
  Les dialogues d'authentification système sont distingués de l'arrière-plan.
- Écritures, pointages, référentiels et brouillon persistés dans la base chiffrée.
  Tous les essais doivent continuer à utiliser uniquement des données fictives.
- Sauvegardes manuelles vers un dossier local choisi. Fichier MTBK v1, AES-256-GCM,
  en-tête authentifié, Argon2id v1.3 (64 MiB, 3 passes, 1 lane). Mot de passe
  de 16 caractères minimum, filtre de secrets évidents, sans estimation d'entropie.
- Matériel dérivé de sauvegarde conservé uniquement dans SQLCipher ; mot de passe
  non stocké en clair. Un nonce aléatoire est généré pour chaque archive.
- Sauvegarde hebdomadaire due après 7 jours, exécutée après déverrouillage. À la
  première configuration, la prochaine ouverture déverrouillée la déclenche.
  Huit archives automatiques suivies dans la base ; manuelles jamais purgées.
- Restauration : authentification de l'archive et validation du schéma avant
  remplacement transactionnel. Sur coffre existant, sauvegarde manuelle vérifiée
  obligatoire avant remplacement. Sur nouveau téléphone, nouvelle clé locale.
- Sauvegardes et transferts Android exclus ; FLAG_SECURE conservé.
- Licences embarquées accessibles dans Réglages.

## Contrôles réalisés

- Compilation APK et APK de test, lint et 19 tests unitaires réussis.
- Sept tests de cryptographie : chiffrement authentifié, mauvais secret, altération,
  troncature, limites et répétition des exports ; BC 1.86 utilisé dans le build.
- Cinq tests d'échange/modèle : aller-retour complet, schéma, références, profondeur,
  taille, UTF-8, vidage à la fermeture et bornes empêchant une écriture illisible.
- Sept tests de saisie monétaire et de modèle existants conservés.
- Relecture indépendante partielle : correction de la validation décimale et de
  l'alignement des limites lecture/écriture avant livraison.
- Installation via tools/play.ps1 : version installée 0.2.0-dev vérifiée, lancement
  confirmé sur l'ASUS après reconnexion Wi-Fi.
- Test instrumenté sur ASUS réussi (base isolée, sans accès au coffre) : fichier
  chiffré sans marqueur texte, mauvaise clé refusée sans remplacement du fichier,
  annulation transactionnelle puis réouverture correcte avec la bonne clé.

Commandes : tools/play.ps1 ; compilation assembleDebugAndroidTest ;
tools/test-vault-device.ps1. Ne pas confondre lancement réussi et validation de
l'authentification ou de l'ergonomie.

## Essais interactifs requis avant validation de ce lot

1. Créer un coffre fictif, s'authentifier, saisir et pointer une écriture.
2. Rotation pendant saisie : contenu conservé, pas de demande d'authentification.
3. Arrière-plan puis retour : contenu fermé, authentification exigée ; écriture
   et brouillon retrouvés après déverrouillage. Essayer annulation et biométrie.
4. Code supplémentaire si configuré : mauvais code refusé, code correct accepté.
5. Réglages : choisir Documents/MaTirelire, dossier du stockage interne. Au retour
   du sélecteur, réauthentifier puis configurer le mot de passe de sauvegarde.
6. Sauvegarder, modifier une écriture, restaurer la sauvegarde avec confirmation :
   état attendu retrouvé. Un mauvais mot de passe doit préserver l'état actuel.
7. Restaurer sur le Samsung à partir du seul fichier et de son mot de passe.
   Le code et la biométrie doivent être réenrôlés, pas copiés depuis l'ASUS.
8. Vérifier captures, aperçu récent, échec d'accès au dossier et stockage plein.
9. Vérifier les archives hebdomadaires et leur rotation en service ; la génération
   n'est pas une tâche exacte en arrière-plan et nécessite une ouverture déverrouillée.

## Limites explicites

- Prototype à données fictives. Aucune certification de sécurité ou revue externe.
- Restauration croisée, invalidation des clés, rotation de huit sauvegardes et
  résistance au stockage plein ne sont pas encore observées en service.
- Les données existent déchiffrées en mémoire pendant la session. Effacement au
  mieux seulement : JVM, Compose, JCA et bibliothèques peuvent garder des copies.
- Le schéma actuel couvre deux comptes de démonstration EUR, 10 000 écritures et
  500 catégories/moyens. Parité Palm et modèle relationnel complet restent à venir.
- Une sauvegarde restaure les données et référentiels existants, pas les secrets
  matériels ni l'autorisation Android d'accès au dossier. Dossier et mot de passe
  des futures sauvegardes doivent être reconfigurés sur un nouvel appareil.
- Les fournisseurs de fichiers cloud sont refusés. Le prototype accepte uniquement
  le fournisseur Android du stockage externe/local, notamment Documents.
- Pas de changements de code supplémentaire après création dans cette version.
- Un changement de verrouillage système peut invalider la clé : disposer d'une
  archive et de son mot de passe. Aucun effacement automatique pour contourner cela.
- Rapports de plantage locaux et gestion plus complète des préférences restent à
  implémenter. Aucun envoi réseau ou identifiant bancaire.
- Pas de CI distante exécutée, dépôt distant toujours non créé.

## Retour arrière

Le commit précédent reste compilable, mais ne sait pas lire le coffre SQLCipher.
Ne pas effacer ou désinstaller ce prototype pour revenir à la démo si une archive
utile doit être conservée. Avant toute migration future, produire et tester une
sauvegarde. Aucun numéro stable, tag ou publication n'est créé par ce lot.

# Retour d'authentification — 0.2.1-dev

9 novembre 2026. Complément au prototype décrit dans VALIDATION-0.2.0.md.

## Problème observé sur 0.2.0-dev

L'utilisateur valide les deux codes supplémentaires identiques, puis le bon
verrouillage Android. L'application affiche à tort une erreur de sauvegarde
altérée. Le dossier du coffre est resté vide : échec avant toute écriture du coffre.
Aucune conclusion sur la classe exacte de l'erreur initiale : elle était masquée
par le message générique et aucun contenu sensible n'a été collecté pour ce diagnostic.

## Traitement corrigé et à confirmer en service

- Le retour d'authentification attend que l'activité soit RESUMED, sans ouvrir les
  données alors que l'application est en arrière-plan.
- Si Android ne renvoie pas le Cipher dans le résultat, réutilisation de l'exacte
  opération cryptographique transmise au système. La clé Keystore exige toujours
  l'authentification par opération ; aucun simple booléen ne déverrouille le coffre.
- Le message de création n'accuse plus une sauvegarde d'être altérée.
- Diagnostic local expurgé : étape technique et classes d'exception uniquement.
  Aucun message d'exception, pile, code, mot de passe, clé ou donnée financière logué.
- Aucun effacement, désinstallation ou remplacement de la clé existante pour
  contourner l'échec.

## Vérifications

Compilation, 19 tests unitaires et lint réussis. Installation et lancement de
0.2.1-dev vérifiés sur l'ASUS par tools/play.ps1.
La nouvelle création avec le bon code Android doit encore être confirmée par
l'utilisateur ; la cause initiale n'est pas déclarée prouvée par ces changements.

Test instrumenté complémentaire compilé : une clé Keystore par opération doit
refuser doFinal sans authentification de cette opération. Pas encore exécuté
pour ne pas interrompre l'essai de création en cours. Le test SQLCipher isolé
avait réussi sur la version 0.2.0-dev.

## Suite de validation

Réessayer création puis verrouillage/réouverture avec des données fictives.
Si l'échec persiste, consulter seulement les diagnostics expurgés MaTirelireVault
pour corriger la cause précise. La restauration croisée et la gestion complète des
sauvegardes restent à observer avant d'accepter des données réelles.

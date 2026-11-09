# Authentification native — 0.2.2-dev

9 novembre 2026. Base : `834d933`, branche dev/vault-0.2.0.

## Diagnostic établi

Le nouvel essai utilisateur sur 0.2.1 a encore échoué. La trace expurgée situe
l'exception à create-seal : IllegalBlockSizeException causée par KeyStoreException,
avant écriture du fichier d'enveloppe. Le dossier du coffre reste vide.
Le retour d'interface Android ne suffit donc pas à conclure que l'opération de
clé a été autorisée. La classe exacte de défaillance système est désormais aussi
journalisée sous forme numérique quand l'API le permet, sans message d'exception.

## Modification

Le parcours utilise directement android.hardware.biometrics.BiometricPrompt,
disponible sur toutes les versions Android prises en charge. L'opération AES-GCM
authentifiée est transmise dans son CryptoObject, avec biométrie forte OU code
système autorisés. L'empreinte reste facultative : l'utilisateur peut choisir le
code du téléphone dans la même fenêtre. Le contrôle logiciel seul ne remplace
jamais l'autorisation de la clé Keystore.

La clé existante et ses paramètres par opération sont conservés. Aucun effacement,
remplacement de clé ou réinitialisation de données. Le comportement à confirmer
reste la création avec le bon code Android et la réouverture du coffre.

## Validation réalisée

- Compilation, 19 tests unitaires et lint réussis.
- Version 0.2.2-dev installée et lancée sur ASUS via le harnais.
- Deux tests instrumentés sur ASUS réussis : SQLCipher, fichier chiffré et mauvaise
  clé refusée sans remplacement ; clé Keystore par opération refusant doFinal
  quand aucune authentification de l'opération n'est fournie.
- Le test Keystore crée et supprime uniquement un alias de test aléatoire ;
  il ne modifie pas la clé de l'application ni le verrouillage du téléphone.
- Réponse interactive de création encore attendue à la rédaction de ce document.
  La correction n'est pas déclarée confirmée par le seul lancement de l'application.

Références :
https://developer.android.com/identity/sign-in/biometric-auth
https://developer.android.com/reference/android/security/KeyStoreException

Toujours un prototype à données fictives, pas une validation de production.

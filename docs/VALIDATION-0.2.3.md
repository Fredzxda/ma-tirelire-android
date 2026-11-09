# Ordre des opérations Keystore et accents — 0.2.3-dev

9 novembre 2026. Base : `25e572a`, branche dev/vault-0.2.0.

## Cause identifiée dans le code

La 0.2.2 a encore échoué avec une empreinte valide. Trace expurgée : create-seal,
IllegalBlockSizeException/KeyStoreException, code public 2, authentification requise.
Le dossier du coffre était encore vide.

Le Cipher Keystore recevait `updateAAD` avant la demande d'authentification. Or
cette méthode transmet déjà des données à l'opération protégée. Le fournisseur
Android peut conserver le refus dans mCachedException sans le lever immédiatement,
puis le lever depuis doFinal, même une fois l'utilisateur authentifié.
Les changements de bibliothèque et de retour d'activité des versions précédentes
ne corrigeaient pas cet ordre erroné dans notre code.

Preuve source consultée : SDK local Android 37, AndroidKeyStoreCipherSpiBase.java,
engineUpdateAAD (vers 526–557) et engineDoFinal (vers 610–612). Source AOSP publique :
https://github.com/aosp-mirror/platform_frameworks_base/blob/main/keystore/java/android/security/keystore2/AndroidKeyStoreCipherSpiBase.java

## Correction

KeyEnvelope sépare explicitement :

1. Préparer le Cipher et son IV, sans lui transmettre de données ni AAD.
2. Faire authentifier cette opération par Android via CryptoObject.
3. Seulement après succès, transmettre l'AAD puis chiffrer/déchiffrer.

Cet ordre s'applique à la création et au déverrouillage. Clé matérielle, paramètres
d'authentification par opération, format et protection AES-GCM inchangés.
Aucun effacement du coffre ou remplacement de clé.

Accents français rétablis dans les messages de création, ouverture, limites,
déverrouillage et bandeau du prototype. Relecture XML et UTF-8 effectuée.

## Validation

- Compilation, 22 tests unitaires et lint réussis.
- Trois tests ciblés KeyEnvelope : préparation sans AAD en chiffrement et
  déchiffrement, aller-retour autorisé et altération refusée. Les tests de
  préparation détectent précisément la réintroduction d'un updateAAD prématuré.
- Installation de 0.2.3-dev et lancement confirmés sur ASUS via tools/play.ps1.
- Tests SQLCipher et refus d'une clé Keystore non authentifiée déjà réussis sur
  la version précédente ; paramètres de clé inchangés dans cette correction.
- Création/réouverture interactive encore à confirmer par l'utilisateur ; cette
  confirmation ne se déduit pas des tests logiciels ni du lancement.

Le prototype reste réservé aux données fictives ; les essais de restauration
croisée et autres limites de VALIDATION-0.2.0.md restent applicables.

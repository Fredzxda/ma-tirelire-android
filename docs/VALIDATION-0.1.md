# Validation locale — 0.1.0-dev

8 novembre 2026. Dépôt permanent : `C:\Dev\ma-tirelire-android`.
Branche initiale : `dev/initial-android`.

## Résultat

APK de démonstration compilé avec le wrapper Gradle 9.4.1, Android Gradle Plugin
9.2.1 et le JDK 25.0.4 fourni avec Android Studio. La distribution Gradle a été
vérifiée contre son SHA-256 officiel, fixé dans les propriétés du wrapper.

- `assembleDebug` : réussi.
- `testDebugUnitTest` : deux tests, aucun échec ; montants FR/EN exacts et rejet des
  entrées ambiguës, exponentielles, négatives ou dépassant les limites.
- `lintDebug` : réussi, zéro erreur ; trois avertissements de versions plus récentes.
- XML valides, mêmes clés de traduction FR/EN.
- APK inspecté avec aapt : identifiant `org.matirelire.android.demo`, minimum API 31,
  cible 37, version `0.1.0-dev`, aucune permission INTERNET.
- `BuildConfig.VERSION_NAME` debug : `0.1.0-dev`, y compris avec stableRelease=true.
- Préparation release stable : tâche de version et BuildConfig donnent `0.1.0`.
  Aucun APK stable ni publication produit par ce contrôle.
- Revue statique indépendante ; correction de la perte des opérations/pointages
  à la rotation via ViewModel en mémoire et de la règle de version debug.

## Contenu livré

Comptes fictifs, liste d'opérations, saisie dépense/revenu en mémoire, simulation du
pointage et écrans de réglages informatifs. Interface adaptative avec navigation
inférieure ou latérale, ressources FR/EN et icône vectorielle originale.
FLAG_SECURE et exclusions déclaratives des sauvegardes cloud/transferts Android.
Écran de reprise de démonstration au retour d'arrière-plan, sans authentification.

## Limites

Pas d'essai sur téléphone ni émulateur, ni validation visuelle ou biométrique.
La CI est préparée mais n'a pas été exécutée sur GitHub. Aucun dépôt distant créé
dans cette étape, aucun push, tag, publication ou installation sur téléphone.
Les protections déclarées doivent encore être observées sur les appareils.
Les opérations restent en mémoire et survivent aux rotations via ViewModel ;
le brouillon de saisie peut être perdu à la rotation ou au passage en arrière-plan.
Les libellés d'exemples déjà initialisés gardent leur langue pendant la session.

Chiffrement du coffre, authentification, sauvegarde/restauration, exports et
parité fonctionnelle complète non implémentés. Ne pas saisir de données réelles.
Ce prototype sert à revoir les parcours, pas à commencer la tenue réelle des comptes.

## Ouvrir et compiler

Dans Android Studio : Open → `C:\Dev\ma-tirelire-android`, sélectionner le JDK
intégré pour Gradle et synchroniser. Dans PowerShell à la racine :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build.ps1
```

APK : `app/build/outputs/apk/debug/app-debug.apk`.
La clé debug est uniquement destinée aux essais ; la signature stable reste à préparer.

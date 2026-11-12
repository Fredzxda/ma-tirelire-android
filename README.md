# Ma Tirelire — Android

Portage Android libre inspiré de [Ma Tirelire Palm de Maxime Soulé](https://github.com/maxatome/ma-tirelire).

**Version actuelle : 0.7.8-dev.** Le projet est en développement ; la parité
fonctionnelle Palm et les validations de la version stable restent en cours.
Android 12 minimum, téléphone et tablette, portrait/paysage, français et anglais.

## Fonctionnalités disponibles

- Coffre local chiffré avec SQLCipher, authentification Android par empreinte
  forte ou code du téléphone, code supplémentaire facultatif.
- Verrouillage en arrière-plan, aperçu protégé et captures d'écran bloquées.
- Sauvegardes chiffrées locales, manuelles et hebdomadaires à la prochaine
  ouverture déverrouillée ; restauration et normalisation des anciennes dates.
- Plusieurs comptes en euros, soldes total et pointé, saisie/modification/suppression.
- Catégories et moyens de paiement réutilisables, virements internes liés.
- Opérations périodiques : limites, anticipation, prévision/à confirmer/confirmée,
  édition de l'occurrence ou du modèle futur, sans modifier les autres écritures.
- Pointage manuel et rapprochement guidé par solde cible, échéances à venir.
- Recherche textuelle combinée à la période, tri, navigation mensuelle et historique.
- Interface compacte avec menus contextuels, indépendante du pointage des séries.

L'application ne demande pas d'accès réseau. Les données financières ne sont pas
envoyées à un serveur. Une sauvegarde reste un fichier chiffré que l'utilisateur
peut copier ailleurs volontairement. Les clés Android propres à l'appareil ne
remplacent pas une sauvegarde restaurable et son mot de passe.

## Ce qui reste à réaliser

Ventilation, devises, catégories avancées, chéquiers, macros, rappels, recherche
avancée, actions groupées, statistiques, imports/exports bancaires, plusieurs bases
et outils de maintenance font partie des lots restants avant 1.0.0.

- [Feuille de route jusqu'à 1.0.0](docs/feuille-de-route-vers-1.0.0.md)
- [Import bancaire prévu : CSV puis OFX/QIF et prévention des doublons](docs/import-releves-bancaires.md)
- [Modèle visuel de la vue Opérations](docs/design/operations-proposition.md)
- [Validation de la version actuelle](docs/VALIDATION-0.7.8-dev.md)

La connexion bancaire directe et iOS sont des chantiers ultérieurs distincts.
Les numéros de versions de la feuille de route sont un découpage proposé,
sans dates de livraison promises.

## Ouvrir et compiler le projet

Ouvrir **la racine du dépôt** dans Android Studio, et non le sous-dossier `app`.
Utiliser un JDK 25 compatible pour Gradle ; le build local utilise celui de Studio.
SDK Android 37 (`platforms;android-37.0`) et Build Tools 36.0.0 nécessaires.
Gradle 9.4.1 et les versions des dépendances sont définis dans le dépôt.

Avec `JAVA_HOME` et le SDK Android configurés :

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
./gradlew -q :app:printAppVersion
```

Sous Windows, remplacer `./gradlew` par `./gradlew.bat`, ou utiliser le wrapper
local si Android Studio est installé dans son emplacement standard :

```powershell
& ./tools/build.ps1 -Tasks @('assembleDebug','testDebugUnitTest','lintDebug','assembleDebugAndroidTest')
```

L'APK de développement est produit dans `app/build/outputs/apk/debug/`.
L'identifiant actuel est `org.matirelire.android.demo` : la migration vers une
future application stable ne doit pas être supposée automatique.

## Versions et vérifications

La version est centralisée dans `gradle.properties`. Les builds portent `-dev`
par défaut, y compris release. L'option `-PstableRelease=true` prépare un numéro
stable mais n'autorise ni publication ni distribution.

La CI compile les APK et exécute les tests unitaires, lint et le contrôle de version.
Elle n'ouvre aucun coffre utilisateur et ne déploie sur aucun téléphone.
Les tests Android matériels et d'interface sont exécutés séparément sur appareil :
115 tests unitaires et 7 tests isolés ASUS passent pour la version 0.7.8-dev.
Cela ne constitue pas une certification de sécurité ni une validation de toutes
les fonctions prévues pour 1.0.0.

Le workflow local `tools/play.ps1` compile, vérifie, installe et lance sur l'ASUS
personnel explicitement autorisé. Son identité est dans `tools/device.local.json`,
ignoré par Git et non fourni. Ce workflow ne choisit aucun autre téléphone et
nécessite Android déverrouillé pour les tests d'interface. Les autres contributeurs
peuvent compiler sans appareil ou employer leur procédure Android Studio/ADB.

## Contribuer

Les contributions sont bienvenues : corrections, tests à données fictives,
traductions et discussions sur les fonctions Palm. Le développement reste sur
des branches `dev/` ; les propositions peuvent être préparées en PR brouillon.

Décrire le comportement attendu, la version, le téléphone/Android concernés et
les étapes de reproduction. **Ne pas publier de relevé bancaire, archive de coffre,
code, mot de passe, clé de signature ou capture contenant des données réelles.**
Les chaînes traduisibles se trouvent dans `app/src/main/res/values*/`.
Les règles de travail et le harnais local sont documentés dans [AGENTS.md](AGENTS.md).
La branche de développement publiée est `dev/android-port`. Le [suivi de publication](docs/publication-github.md)
explique la préservation de l'historique et la confidentialité des e-mails.

## Origine et licence

Projet nouveau sous **GPL-3.0-or-later**, voir [LICENSE](LICENSE).
Inspiré des fonctionnalités de Ma Tirelire Palm. Aucun code, logo ou exécutable
Palm n'est repris dans cette version Android. Le dépôt historique annonce une
GPL sans numéro ; toute reprise future doit être examinée séparément.

Les bibliothèques Android/Kotlin, SQLCipher, Bouncy Castle et les outils Gradle
restent soumis à leurs licences respectives ; voir notamment les notices dans
`app/src/main/assets/licenses.txt`. La licence du projet ne remplace pas les leurs.

Le [cadrage initial](docs/cadrage-2026-11-08.md) et les audits dans `docs/`
distinguent sources historiques, exigences validées et adaptations Android.

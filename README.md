# Ma Tirelire — Android

Démonstration **0.1.3-dev** du portage de Ma Tirelire Palm.
Android 12 minimum, téléphone et tablette, français et anglais.

## Utilisation de cette version

Navigation, comptes fictifs, liste d'opérations, saisie en mémoire et pointage de
démonstration. Solde total et pointé visibles, catégories et moyens réutilisables,
brouillon conservé à la rotation et pendant la pause dans la mémoire du ViewModel.
Les modifications disparaissent à la fermeture du processus.
**Ne pas saisir de données réelles.** Chiffrement, authentification biométrique,
sauvegarde/restauration et parité Palm ne sont pas encore implémentés.
Le masque au retour d'arrière-plan est un écran de reprise de démonstration,
pas une authentification. Aucun accès réseau demandé.

## Développement

Ouvrir ce dossier dans Android Studio, sélectionner son JDK intégré pour Gradle,
puis synchroniser. SDK 37 et Build Tools 36 nécessaires ; la chaîne est figée dans
les fichiers Gradle. Sur ce PC : `powershell -NoProfile -File tools/build.ps1`.
Le script utilise le JDK de Studio sans modifier Java globalement.

Commandes portables avec un JAVA_HOME compatible :

```
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew :app:printAppVersion
```

La version centrale est dans `gradle.properties`. Tout build porte `-dev` par
défaut, y compris release ; seul `-PstableRelease=true` prépare un numéro stable.
Cela n'autorise aucune publication. L'identifiant `.demo` sépare cette app du
futur coffre stable ; aucun transfert automatique de données n'est promis.

## Origine et licence

Projet nouveau sous GPL-3.0-or-later. Inspiré des fonctionnalités de
[Ma Tirelire Palm de Maxime Soulé](https://github.com/maxatome/ma-tirelire).
Aucun code, logo ou exécutable Palm repris dans cette démonstration. Le dépôt
historique annonce une GPL sans version ; toute reprise future devra faire
l'objet d'une vérification des licences. Bibliothèques Android/Kotlin sous leurs
licences respectives ; la licence du projet ne remplace pas leurs notices.

Le cadrage et les parcours sont dans `docs/`. Ils distinguent sources historiques,
exigences validées, adaptations proposées et règles restant à établir.

Les résultats et limites sont dans [docs/VALIDATION-0.1.3.md](docs/VALIDATION-0.1.3.md).
Les opérations périodiques restent suivies dans [REC-001](docs/operations-periodiques.md)
et seront implémentées dans un lot ultérieur.

## Compiler et installer sur un telephone

Activer le debogage USB dans les options developpeur du telephone, le brancher et
accepter l'autorisation de cet ordinateur. Relever son identifiant avec :

```powershell
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" devices -l
```

Puis, a la racine du projet :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-install.ps1 -DeviceSerial IDENTIFIANT
```

Le script compile, execute tests et lint, puis installe uniquement si les controles
passent. Il cible explicitement le telephone choisi et verifie la version installee.
Il ne desinstalle pas l'application en cas d'echec, ne redemarre pas le telephone et
ne lance pas l'application. La mise a jour peut arreter la session en memoire de
la demonstration. Le script build.ps1 seul conserve son comportement sans installation.
Validation du nouveau script : syntaxe PowerShell controlee ; installation physique
pas encore testee. Reference : https://developer.android.com/tools/adb
## Workflow Play autorise sur ASUS

Depuis la racine : `powershell -NoProfile -File tools/play.ps1`.
Ce workflow remplace le bouton Run pour nos livraisons locales : compilation,
sept tests metier, lint, installation de la demo, verification de version et lancement.
La cible est epinglee par son identite physique dans `tools/device.local.json`,
ignore de Git ; sa connexion Wi-Fi courante est retrouvee automatiquement.
Aucune selection d'un autre telephone si l'ASUS est absent/non autorise ou si deux
connexions vers lui sont actives. Ne pas couper une connexion automatiquement.
Les futurs agents doivent suivre AGENTS.md et utiliser ce workflow pour chaque
nouvelle version locale validee, tant que le telephone est connecte.
Ce n'est pas un observateur global de toutes les compilations Gradle : un build CI
ou build.ps1 seul ne declenche pas une installation.

Validation du 9 novembre 2026 : cinq scenarios de selection simules reussis via
`powershell -NoProfile -File tests/check-device-selection.ps1` ; workflow complet
execute sur ASUS en Wi-Fi, installation 0.1.3-dev et `am start -W` Status ok.
Ce lancement ne constitue pas une revue visuelle du formulaire.
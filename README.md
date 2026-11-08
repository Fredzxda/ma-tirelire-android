# Ma Tirelire — Android

Première démonstration **0.1.0-dev** du portage de Ma Tirelire Palm.
Android 12 minimum, téléphone et tablette, français et anglais.

## Utilisation de cette version

Navigation, comptes fictifs, liste d'opérations, saisie en mémoire et pointage de
démonstration. Les modifications disparaissent à la fermeture du processus.
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

Les resultats et limites des controles locaux sont dans [docs/VALIDATION-0.1.md](docs/VALIDATION-0.1.md).

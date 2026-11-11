# Ma Tirelire — Android

Prototype de sécurité **0.6.0-dev** du portage de Ma Tirelire Palm.
Android 12 minimum, téléphone et tablette, français et anglais.

## Utilisation de cette version

Recherche textuelle dans les operations du compte : description, categorie et
moyen de paiement, accents et casse ignores. Somme des resultats distincte des
soldes globaux ; recherche effacee au verrouillage. Les filtres avances restent
a venir.

Statuts sans popup de portée quand les champs sont inchangés : Prévision →
À confirmer → Confirmée en deux actions. Le statut intermédiaire choisi est
persisté même avant la date prévue, sans pointage automatique. Schéma 9 ;
archives précédentes lisibles, pas de rétrogradation.

Anticipation réglable : à échéance (défaut), fin du mois, 15/30 jours. Les
occurrences anticipées sont enregistrées et incluses dans le solde total. États
Prévision / À confirmer / Confirmée, indépendants du pointage bancaire. Les
prévisions futures non confirmées sont hors des flux de revenus/dépenses.
Réduire l’horizon conserve les écritures existantes. Réglage chiffré sauvegardé
avec le coffre ; schéma 8 si anticipation active, anciennes archives lisibles.

Échéances périodiques à venir par compte sur 30, 90 ou 365 jours : consultation
en lecture seule des séries actives, sans génération ni modification des soldes.
Les opérations déjà enregistrées ne sont pas répétées dans cette vue.

Si seuls la fréquence, la fin ou le nombre restant changent, la série est
enregistrée directement : aucun dialogue et aucune modification de cette écriture.

Fin de périodicité au calendrier avec affichage localisé. Les échéances générées
sont à confirmer : enregistrer leur fiche demande de choisir cette opération seule ou le modèle
futur. Aucune autre écriture déjà enregistrée, même en attente, ne change.
Confirmation distincte du pointage bancaire. Format comptable v7, anciennes
sauvegardes lisibles ; pas de rétrogradation.

Opérations périodiques : fréquences historiques, fin par date ou nombre total,
génération des échéances dues après déverrouillage et pendant la session ouverte.
Gestion des séries dans Réglages ; choix entre occurrence seule et futures
échéances lors des modifications. Les occurrences générées sont non pointées.
Format comptable v7 : anciennes sauvegardes lisibles, pas de rétrogradation.

Choisir Virement dans les moyens de paiement pour afficher le compte facultatif
à côté : sans compte, écriture externe simple ; avec compte, paire interne.
Conversion d’une dépense ou recette existante possible, identité et pointage
conservés, contrepartie non pointée. Les virements internes sont hors du périmètre
des statistiques de revenus/dépenses ; leur catégorie est masquée.

Virements internes EUR liés : deux écritures opposées en une saisie ; modification
cohérente depuis chaque côté, pointages indépendants, suppression de la paire
après confirmation. Les comptes d’un virement enregistré sont fixes dans ce lot.
Le format comptable v3 des virements reste incompatible avec les anciennes apps.

Date modifiable dans la saisie et la modification. Une nouvelle saisie propose
la date du jour ; le brouillon garde la date choisie. Les anciens libellés de
date sont conservés sans interprétation et peuvent être remplacés explicitement.

Appui sur une opération pour la modifier ; corbeille dans la fiche uniquement.
Nouveautés et points à tester affichés une fois par version après déverrouillage,
et consultables à nouveau dans les réglages.

Premier lot 0.3 : modification et suppression des opérations, avec recalcul
des soldes et confirmation avant suppression. Les dates et le pointage sont
conservés lors de la modification. Brouillons de modification chiffrés.
Authentification Android automatique au retour ; bouton de reprise si annulée.
Comptes personnalisables en euros : création, nom et solde initial.
Pointage guidé : solde cible, sélection provisoire et confirmation à écart nul.
Annuler ou verrouiller abandonne la session non confirmée. Les devises restent
pour les lots suivants.
Les sauvegardes historiques sont lisibles ; le nouveau format comptable v2
ne doit pas être ouvert avec une ancienne version de l’application.

Comptes fictifs et parcours de saisie conservés. Le contenu est maintenant
persisté dans un coffre SQLCipher, protégé par l'authentification Android.
Empreinte facultative et code supplémentaire facultatif à la création.

Dans Réglages : dossier local de sauvegarde, mot de passe distinct (16 caractères
minimum), export chiffré et restauration. L'hebdomadaire due se fait à l'ouverture
déverrouillée ; huit archives automatiques sont conservées, les manuelles restent.
Le sélecteur de dossier peut verrouiller le coffre : réauthentifier au retour,
puis configurer la sauvegarde. Les archives précédentes gardent leur ancien mot
de passe si celui-ci est changé pour les prochains exports.

**Utiliser uniquement des données fictives.** Le prototype et sa restauration
croisée restent en cours de validation. Aucun accès réseau demandé.
Le mot de passe d'une archive perdue ne peut pas être réinitialisé ; conserver
séparément une copie du fichier chiffré si le téléphone risque d'être perdu.

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

Les résultats et limites du coffre sont dans [docs/VALIDATION-0.2.0.md](docs/VALIDATION-0.2.0.md) et le suivi du retour Android dans [docs/VALIDATION-0.2.1.md](docs/VALIDATION-0.2.1.md).
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
les tests metier et cryptographiques, lint, installation de la demo, verification de version et lancement.
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
Suivi de l authentification native et des tests materiels : [VALIDATION-0.2.2](docs/VALIDATION-0.2.2.md).

Correction de l ordre des operations Keystore et des accents : [VALIDATION-0.2.3](docs/VALIDATION-0.2.3.md).

Ouverture du coffre confirmee sur ASUS et controle ajoute au Play : [VALIDATION-0.2.4](docs/VALIDATION-0.2.4.md).

# Publication GitHub du projet

Dépôt créé pour le code source public :
https://github.com/Fredzxda/ma-tirelire-android

Branche de publication et de développement : `dev/android-port`.
Aucune création ni modification de `main`, aucun tag ni release stable dans
cette première publication. La version applicative reste 0.7.4-dev.

## Historique et confidentialité des e-mails

Le premier push a été refusé par GitHub (GH007) pour préserver une adresse
e-mail privée dans les métadonnées des commits. Cette protection n'a pas été
désactivée et l'adresse privée n'est pas rendue publique.

Une copie isolée des 45 commits locaux a été préparée avec l'adresse GitHub
`37531742+Fredzxda@users.noreply.github.com` pour les champs auteur et committer.
Les noms d'auteur, dates, messages, fichiers et relations de parenté sont
préservés. Tous les arbres de fichiers ont été comparés et sont identiques.
Les SHA changent nécessairement après ce changement de métadonnées.

La table de correspondance entre SHA d'origine et SHA publics est conservée
localement dans `.git/history-publication.tsv`, sans publication de cette table.
Les anciens identifiants mentionnés dans les documents de validation peuvent
donc désigner l'historique local d'origine ; sur GitHub, retrouver le commit par
son message et sa version. Les commits ajoutés après cette préparation ont
directement un e-mail noreply.

Les anciennes branches locales n'ont pas été réécrites ni supprimées. Elles
restent une sauvegarde de l'historique d'origine et ne doivent pas être poussées
avec `--all` : leur métadonnée d'e-mail est encore privée. Continuer le travail
sur `dev/android-port`, avec l'e-mail noreply configuré seulement pour ce dépôt.

## Contenu contrôlé

Avant publication : 44 commits et 364 blobs uniques accessibles contrôlés par
recherche heuristique de secrets et de fichiers privés, sans alerte ; commit
de préparation public contrôlé séparément dans l'index. Les fixtures et aperçus
de conception contiennent des exemples fictifs. Cette vérification n'est pas
une garantie absolue d'absence de tout secret ni un audit de sécurité applicatif.

Les clés de signature, données de coffre, archives, local.properties, identité
du téléphone, caches/builds et réglages Studio locaux restent exclus. Les
modifications locales sans rapport ne sont pas ajoutées au commit de publication.

La CI existante compile les APK application/tests, exécute tests unitaires et
lint, vérifie le suffixe -dev. Elle ne lance pas les tests physiques ASUS, ne
publie pas d'APK ni de release et ne contient pas d'identifiant bancaire.
Ses résultats sont consultables dans l'onglet Actions du dépôt public.

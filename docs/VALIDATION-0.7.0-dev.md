# 0.7.0-dev — premier lot personnalisation

Tri global des operations : saisie (defaut conserve), date recente, date ancienne.
Tri de presentation uniquement : stable a date egale, dates historiques illisibles
en fin de liste, meme ordre dans la recherche. Aucun calcul comptable modifie.
Reglage dans le snapshot chiffre, sauvegarde/restaure ; schema 10 si tri personnalise.
Lecture des schemas 1 a 9 avec ordre original par defaut.
Paysage : titre bleu fonce, rail avec fond sur toute la hauteur.

Essais utilisateur : menu en paysage, tris et recherche, verrouillage/reouverture,
sauvegarde/restauration, soldes inchanges, retour au tri initial.
Les autres fonctions 0.7 (multibases, purge, widgets, raccourcis) restent a venir.

Retour arriere : commit precedent 897296d. Sauvegarde chiffree avant evolution ;
ne pas retrograder une base schema 10 vers une ancienne application qui la refuse.
Ne pas desinstaller l application pour revenir en arriere.

Validation finale : compilation, 100 tests unitaires et lint reussis.
ASUS : installation 0.7.0-dev verifiee, trois tests SQLCipher/Keystore isoles
reussis et application relancee. Validation visuelle utilisateur encore attendue.

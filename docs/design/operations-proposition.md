# Vue Opérations — proposition du 11 novembre 2026

Statut : **à valider avant implémentation**. Application actuelle inchangée :
0.7.2-dev, commit `7f73139`. La maquette emploie uniquement des données fictives.

Ouvrir [la maquette interactive](operations-proposition.html) dans un navigateur.
Elle montre portrait et paysage ; les menus, le tri et la recherche sont essayables.
Les fiches et fonctions spécialisées sont simulées par un message, sans stockage.

## Organisation proposée

1. Compte et soldes fixes. En paysage : marque/version dans le rail bleu foncé.
2. Une ligne fixe : Tri (icône), Compte, Filtre, Recherche, Fonctions.
3. Une seule ligne contextuelle temporaire sous le menu. Elle se ferme en
   retouchant son entrée, en ouvrant une autre entrée ou en cliquant en dehors.
4. Liste seule défilante, reflétant le compte, la période ET la recherche actifs.

Sans menu ouvert, rien ne subsiste de l'ancienne rangée de comptes, des contrôles
mensuels et des boutons de fonctions : ils vivent dans leur ligne contextuelle.
Le filtre affiche discrètement sa période dans le menu ; un indice sur Recherche
indique qu'un texte reste actif après fermeture. Aucun texte explicatif permanent.
Les explications passent dans Informations, avec les règles de soldes et prévisions.

## Comportements à conserver

| Action | Résultat |
|---|---|
| Tri | Applique le tri et change l'icône immédiatement |
| Compte | Affiche les comptes dans la ligne contextuelle |
| Filtre | Affiche mois précédent/suivant, courant, historique, dates à vérifier |
| Recherche | Affiche un champ intégré ; met la liste à jour sans popup |
| Fonctions | Affiche les actions « Pointer avec un relevé », « Échéances à venir » |
| Clic hors ligne | Ferme la ligne en gardant les sélections et le texte |
| Clic sur carte avec une ligne ouverte | Ferme la ligne ET ouvre la fiche |
| Clic sur pointage | Ferme la ligne ET pointe une fois, sans ouvrir la fiche |
| Effacer la recherche | Retire ce critère ; garde la période et le compte |

**Changement fonctionnel demandé** : contrairement à la 0.7.2, la recherche
s'applique à la période choisie. Pour rechercher partout, choisir Tout l'historique.
Ce changement ne modifie jamais les soldes globaux, les écritures ni les échéanciers.
Une recherche active doit rester identifiable quand sa ligne est refermée.

Interactions à implémenter : zones tactiles de 48 dp, texte des boutons et labels
traduits FR/EN, navigation clavier/accessibilité, conservation à la rotation,
effacement du texte sensible au verrouillage. Ne pas utiliser de voile absorbant
le clic extérieur. Le clavier ne ferme pas la ligne de recherche.

## Carte d'opération

| Gauche | Droite |
|---|---|
| Statut de série, si applicable | Date |
| Description, alignée en haut | Montant, aligné en haut |
| Catégorie · moyen de paiement | Case et libellé Pointée / Non pointée |

Le montant et la description démarrent sur la même ligne. La description n'est
plus centrée sur le bloc montant/pointage. Le pointage reste sous le montant,
avec une zone tactile séparée ; la carte ouvre toujours la fiche de modification.
Les virements internes conservent leur indication de compte lié, sans catégorie
inventée. Descriptions longues, noms de comptes longs et grands montants doivent
être essayés au texte agrandi avant de fixer les dimensions réelles.

## Périodicité

Libellés courts proposés : Date de fin et Occurrences restantes. Toujours placés
dans l'encoche du cadre, qu'il soit vide, rempli ou en cours de saisie. Même style
et même hauteur de contenu. La date reste un sélecteur de calendrier ; le nombre
reste un champ numérique. À grande taille de texte, les deux cadres grandissent
ensemble ou passent l'un sous l'autre. Les dimensions HTML sont des indications
visuelles, pas une contrainte à recopier telle quelle en Compose.

## Portée de la maquette

Les deux panneaux sont des démonstrations indépendantes, pas une simulation de
rotation d'un même coffre. Le pointage emploie les mêmes données fictives en mémoire.
Les champs de périodicité illustrent le placement des labels ; leur calendrier
n'est pas implémenté. Aucun asset externe, accès réseau ni donnée personnelle.
Les soldes affichés sont des exemples fixes, pas des calculs comptables du prototype.

Validation attendue : disposition des cartes, ligne de commandes, principe de
recherche combinée au filtre, et labels de périodicité. Après accord, implémenter
dans une version -dev et vérifier sur l'ASUS ; ce document n'annonce aucun déploiement.

Autre choix propose : placer le bouton + dans le bandeau fixe pour ne pas
recouvrir les cartes ou leur pointage. Aucun comportement metier modifie.

Apercus : operations-proposition.png (menus fermes), operations-menus.png
(recherche portrait et fonctions paysage ouvertes). Rendus dans Chrome local
en mode sans fenetre et relus visuellement ; syntaxe JavaScript verifiee par Node.
Skill projet et copie installee valides par quick_validate.py. Ce controle
structurel ne vaut pas validation de l ergonomie par Frederic. Aucun test ou
deploiement Android realise : aucun fichier applicatif n a ete modifie.

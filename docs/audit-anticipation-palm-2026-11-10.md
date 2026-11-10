# Audit Palm : anticipation et etats des operations periodiques

Verification statique du 10 novembre 2026, sources maxatome/ma-tirelire master.
Pas d'essai sur Palm ni d'emulateur. Application Android actuelle : 0.5.4-dev.

## Generation anticipee : confirmee, mois courant non impose

Transaction.m 3150-3159 calcule les repetitions jusqu'a aujourd'hui + ul_repeat_days
lorsque ul_auto_repeat est actif. computeAllRepeats utilise le meme horizon par
defaut (3205-3206). SumListForm.m 144-145 appelle aussi cette fonction avec 0,
c'est-a-dire cet horizon, pour l'action manuelle d'execution des repetitions.

DBasePropForm.m 440-447 active l'automatisme et initialise l'avance a 15 jours
pour une nouvelle base. Les lignes 248-254 enregistrent un nombre configurable,
plafonne a 127 jours. obj-fr.rcp 411-413 expose l'execution automatique et la
visualisation N jours avant. Les sources consultees ne prouvent donc pas une
generation systematique jusqu'au dernier jour du mois civil courant. Un horizon
configure pouvait toutefois donner cette impression ou couvrir le mois entier.

Transaction.m 3020-3045 montre la creation d'un nouvel enregistrement, distincte
d'un simple affichage de projection. Le marqueur de repetition passe a la
nouvelle operation et est retire de l'ancienne (3120-3129). Ne pas interpreter
ce marqueur comme une preuve d'une confirmation utilisateur.

## Reperes visuels et pointage : independance confirmee, sens a distinguer

Transaction.h 103-110 definit des bits distincts pour checked, marked et repeat.
L'interface francaise appelle checked Pointe, et fournit Marquer/De-marquer
ainsi que Marques vers pointes (obj-fr.rcp 44-52, 97-100, 195).

TransScrollList.m 485-491 applique une couleur et/ou du gras configurables aux
repetitions ; 650-654 dessine une icone de repetition. Le pointage a son propre
rendu (881-907), et le marquage un autre symbole (913-917). Les trois mecanismes
sont distincts. Cela ne prouve pas que Marque equivalait automatiquement a
Confirme pour une occurrence : son libelle est generique et il sert aussi aux
filtres et actions groupees. Aucun champ dedie amountConfirmed n'a ete trouve
dans la structure d'operation historique examinee.

Il existait aussi une vue de projections : RepeatsListForm.m 52-59 initialise sa
date limite a aujourd'hui + N jours. TransScrollList.m 983-1000 gere explicitement
une operation future qui n'existe pas encore, et 892-895 n'affiche pas cette
projection comme pointee. Il faut distinguer cet affichage virtuel de la
generation anticipee d'enregistrements ci-dessus.

## Comparaison avec Android 0.5.4-dev

- Generation actuelle uniquement des echeances dues, pas des jours suivants.
- Echeances futures dans une vue distincte en lecture seule 30/90/365 jours.
- amountConfirmed distinct de cleared : confirmation et pointage independants.
- La liste indique A confirmer mais une occurrence confirmee porte seulement
  Occurrence periodique. La distinction explicite Confirmee manque donc en UI.

Une prochaine evolution pourra proposer une avance configurable (dont fin du
mois courant) et trois libelles visibles : Prevision, A confirmer, Confirmee,
avec Pointage toujours separe. Ce serait une adaptation Android explicite,
pas la preuve du libelle ou du reglage Palm exact. Definir les effets sur soldes,
modification de serie et projections avant toute generation anticipee reelle.

## Sources

- https://github.com/maxatome/ma-tirelire/blob/master/Transaction.m#L3150
- https://github.com/maxatome/ma-tirelire/blob/master/Transaction.m#L3020
- https://github.com/maxatome/ma-tirelire/blob/master/DBasePropForm.m#L440
- https://github.com/maxatome/ma-tirelire/blob/master/DBasePropForm.m#L248
- https://github.com/maxatome/ma-tirelire/blob/master/Transaction.h#L103
- https://github.com/maxatome/ma-tirelire/blob/master/TransScrollList.m#L485
- https://github.com/maxatome/ma-tirelire/blob/master/TransScrollList.m#L650
- https://github.com/maxatome/ma-tirelire/blob/master/TransScrollList.m#L881
- https://github.com/maxatome/ma-tirelire/blob/master/TransScrollList.m#L983
- https://github.com/maxatome/ma-tirelire/blob/master/RepeatsListForm.m#L52

Audit documentaire uniquement : aucun code, coffre, version ou deploiement modifie.

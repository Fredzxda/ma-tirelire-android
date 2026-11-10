# Validation 0.3.3-dev

Comptes en EUR : ajouter depuis Comptes ; crayon pour nom et solde initial.
Maximum 50 comptes, nom unique sans distinction de casse. Solde initial signe,
zero accepte, deux decimales au plus selon la langue du telephone.
Le solde initial contribue au solde total et pointe. Les operations ne changent pas.

Le format comptable passe a schema 2 quand les noms sont enregistres. Les
archives schema 1 restent importables et recuperent les deux noms historiques.
Une ancienne application ne lit pas le schema 2 : ne pas retrograder. Les
archives precedentes sont conservees ; restauration avec cette version possible.
La restauration d'un ancien export remplace les comptes et leurs operations.

Les nouvelles fiches de compte survivent a la rotation dans le ViewModel, mais
leurs modifications non enregistrees sont effacees au verrouillage. Les brouillons
d'operations gardent leur comportement persiste dans le coffre chiffre.

Essais : creation avec solde negatif, saisie dans le nouveau compte, renommage,
recalcul des deux soldes, refus des doublons et decimales excessives, rotation,
verrouillage/reouverture, sauvegarde/restauration incluant les noms et soldes.
Les tests automatises couvrent migration, references, limite 50 et montants FR/US.
La validation visuelle sur appareil reste distincte des tests de stockage isoles.

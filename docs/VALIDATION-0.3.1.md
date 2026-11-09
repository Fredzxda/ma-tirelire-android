# Validation 0.3.1-dev

- Appui sur la carte : fiche de modification pre-remplie, sans bouton Modifier.
- Pointage sur la ligne/checkbox : pas d'ouverture de fiche.
- Corbeille uniquement en modification, absente en nouvelle saisie.
- Confirmation affichee au-dessus de la fiche ; annulation sans effet ; confirmer
  supprime l'operation et ferme la fiche, avec recalcul des soldes.
- Rotation en modification : brouillon conserve.
- Nouveautes FR/EN apres deverrouillage, une fois par version apres acquittement.
  Rotation avant acquittement : restent visibles. Retour apres acquittement :
  pas de reaffichage. Reglages : reouverture possible.
- Annuler la demande d'authentification Android : pas de relance en boucle ;
  bouton Deverrouiller pour reessayer. Aucun code a transmettre.

L'acquittement de version est conserve dans le coffre chiffre, propre a cet
appareil, hors du contenu financier exporte. Ni migration ni effacement du coffre.

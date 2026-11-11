# Validation 0.6.1-dev

## Ergonomie du compte

- Bandeau prototype retire de la session ouverte.
- Soldes total et pointe de meme typographie, cote a cote, hors de la liste
  defilante, avec le nom du compte selectionne.
- Explication des previsions accessible via Informations pres des soldes.
- Bouton Rechercher au meme niveau que Pointer avec un releve et Echeances.
  Fenetre dediee protegee, liste de resultats defilante, toucher un resultat
  ferme la recherche et ouvre la fiche editable. La liste principale reste complete.
- Fin de serie : deux OutlinedTextField de meme hauteur et typographie ;
  le champ date en lecture seule ouvre le calendrier, nombre editable.
  Date courante du brouillon relue a chaque ouverture du calendrier.

## Verification manuelle attendue

Portrait et paysage : defiler une longue liste et verifier les soldes fixes ;
ouvrir Informations ; rechercher puis modifier une operation ; rouvrir la
recherche, faire une rotation, verifier clavier et fermeture ; editer une serie,
choisir/retirer une fin, saisir un nombre restant, enregistrer et rouvrir.
Tester aussi une taille de texte Android agrandie et les grands montants.
La suppression du bandeau ne constitue pas une certification de securite.

## Retour arriere

Version precedente sauvegardee avant modification dans e12db08 (0.6.0-dev).
Aucun changement de schema du coffre, aucune suppression de donnees.
Conserver une sauvegarde chiffree ; ne pas desinstaller pour revenir en arriere.
Validation realisee : build, 97 tests unitaires et lint reussis. ASUS :
version installee 0.6.1-dev verifiee, 3 tests de stockage isoles reussis,
lancement confirme. Validation visuelle utilisateur encore attendue.

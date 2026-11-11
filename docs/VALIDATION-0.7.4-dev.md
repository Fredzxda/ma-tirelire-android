# 0.7.4-dev — indications compactes du menu Operations

Filtre : Historique / A verifier dans le menu, sur une seule ligne ; les choix
conservent leurs libelles complets. La description d etat accessible expose la
periode complete. Recherche : indication Active au lieu du point, retiree a
l effacement du texte, conservee quand la ligne de recherche est fermee.
Aucun changement des criteres, du tri, des montants ou du schema du coffre.

Test Compose existant etendu : caption Historique lisible sans debordement,
une seule ligne, hauteur de menu identique avant recherche, apres saisie, apres
selection de l historique et apres effacement ; etat Active present puis retire.
Tests de carte/pointage, fermeture et periodicite conserves.
Essais utilisateur : historique et recherche en portrait/paysage, fermeture,
effacement ; les criteres doivent continuer a se combiner.

Retour arriere code : 752db76 (0.7.3-dev) ; sauvegarde chiffree conservee,
aucun changement de format, ne pas desinstaller.

Validation finale : compilation, 115 tests unitaires et lint reussis. Installation
0.7.4-dev verifiee sur ASUS, sept tests isoles reussis, application relancee.
Le test mesure le texte visible (une ligne sans ellipse et largeur des glyphes),
la largeur reservee du paragraphe ne prouvant pas un texte coupe.
Premier essai UI interrompu par Android verrouille : harnais avec diagnostic
explicite ajoute, activite fictive seule maintenue allumee pendant les tests.
Aucun reglage de veille/verrouillage du telephone modifie. Rendu personnel a
confirmer par Frederic en portrait et paysage.

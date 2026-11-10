# Validation 0.4.1-dev

Les cinq essais des virements 0.4.0-dev ont ete valides par Frederic.
Moyen de paiement Virement avec compte facultatif adjacent, suppression de la coche.
Aucun compte : virement externe ordinaire, inclus dans les flux de statistiques.
Compte distinct : paire interne exclue des statistiques de revenus/depenses et
par categorie. Le perimetre est centralise par reportingTransactions(), pas par
une comparaison de libelle ou une simple compensation des deux montants.
Aucun ecran de statistiques n'est ajoute dans ce lot. La categorie interne est
conservee pour compatibilite, mais masquee dans la fiche et ignoree par ce perimetre.

Role du moyen Virement stocke dans le coffre (schema 4), independant de la langue
actuelle du telephone. Migration des schemas 1/2/3 : reutiliser Virement/Transfer
existant, sinon ajouter le moyen localise. Les anciens virements internes avec
Carte recoivent ce moyen ; les operations simples Carte ne changent pas.
Les anciens moyens et categories restent disponibles. Archives anciennes lisibles.

Conversion d'une operation simple existante en paire : conserver son id, compte,
montant/sens, date et pointage ; creer un seul nouvel id oppose non pointe.
Un revenu peut donc devenir le cote crediteur d'un virement interne. Une ecriture
de snapshot SQLCipher pour la conversion, aucune contrepartie avant Enregistrer.
Annuler ne transforme pas l'operation. Capacite : une seule ligne supplementaire.
Un virement deja lie conserve ses comptes, son sens et son moyen designe. Le
passage lie vers externe n'est pas propose : supprimer la paire puis recreer.

Essais : moyen Virement sans/avec cible, conversion depense/recette pointee,
annulation, conservation apres rotation/verrouillage/restauration, anciens moyens,
migration, rejet Carte pour une paire, exclusion des deux cotes du reporting.
Ne pas retrograder une application sur le schema 4.

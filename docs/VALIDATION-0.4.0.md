# Validation 0.4.0-dev

Virements internes EUR en une saisie : debit dans le compte choisi et credit
oppose dans le destinataire. Montant positif et comptes distincts obligatoires.
Deux identifiants reciproques ; schema 3 des qu'un virement ou brouillon de
virement existe. Schemas 1 et 2 toujours importables. Pas de conversion implicite
des anciens moyens de paiement nommes Virement. Ne pas retrograder.

Edition depuis chaque cote : montant, date, libelle, categorie et moyen partages.
Pointage independant et conserve. Comptes et sens fixes apres creation : pour
les changer, supprimer la paire puis la recreer. Transformation d'une operation
simple existante en virement non proposee dans ce premier lot.

Creation/edition : valider la paire complete avant mutation, un seul appel de
persistance, un snapshot dans une transaction SQLCipher. Suppression : deux
cotes dans un seul snapshot apres confirmation explicite. Echec de persistance :
le controleur verrouille le coffre ; la transaction SQL conserve l'ancien etat.
Archives : refuser liens orphelins/asymetriques, meme compte, montants non opposes,
date/libelle/references incoherents et brouillons incompatibles avec leurs liens.

Essais manuels dans les nouveautes : creation, consolidation, edition cote credit,
pointage independant, suppression annulee/confirmee, rotation, verrouillage,
sauvegarde/restauration. Devises et repetitions de virements : lots ulterieurs.

# Validation 0.5.5-dev

Anticipation explicite et configurable : DUE par defaut (comportement precedent),
MONTH_END (dernier jour civil du mois), DAYS_15, DAYS_30. Inclut la borne finale,
respecte arrets, comptes, limites de serie et calendrier rebase. Generation apres
deverrouillage/en session, jamais quand le coffre est verrouille. Snapshot atomique
avec curseurs et paires ; budget et limites du moteur conserves.

Les previsions generees sont de vraies lignes incluses dans le solde total.
Pas de pointage automatique. Etat visuel derive de seriesId, amountConfirmed et
date civile : future non confirmee = Prevision ; due non confirmee = A confirmer ;
confirmee = Confirmee meme si future. Checked reste independant. Texte et couleurs
distincts ; un rappel des effets sur le solde apparait si des previsions existent.
Les previsions futures non confirmees sont exclues du perimetre revenus/depenses,
ainsi que les deux cotes des virements internes. Pointage inchange par la generation.

Le reglage appartient au LedgerData chiffre et aux sauvegardes (schema 8 si actif,
anciens schemas conserves par defaut). Restaurer ne duplique pas les occurrences.
Reduire l'horizon/arreter une serie ne supprime pas les lignes deja enregistrees.
L'edition de serie conserve strictement toutes les autres lignes, meme previsions.
UpcomingOccurrences part du prochain curseur : pas de double affichage de ces
previsions dans la vue des echeances non encore enregistrees.

Essais : fin de mois avec hebdomadaire, bornes 15/30 jours, libelles independants
du pointage, confirmation avant echeance, diminution d'horizon, virement, edition
de serie avec historique futur protege, sauvegarde/restauration et non-duplication.

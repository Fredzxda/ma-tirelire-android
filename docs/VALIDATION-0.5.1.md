# Validation 0.5.1-dev

Date de fin : calendrier natif protege FLAG_SECURE, meme presentation locale
que la date d'operation. Valeur ISO interne. Minimum : debut de serie. Annuler
ne modifie rien. Choisir une date efface le nombre, saisir un nombre efface la
date ; retirer la date revient au mode sans limite si aucun nombre.

Confirmation d'occurrence distincte du pointage bancaire : nouvelles occurrences
amountConfirmed=false, premieres ecritures de serie confirmees par leur creation.
Ouvrir une occurrence en attente propose Confirmer l'occurrence. Le montant
confirme modifie le modele futur et les occurrences de numero superieur encore
non confirmees, sans pointage sur aucun cote ; les historiques/confirmations
precedents restent inchanges. Pour une paire, confirmation des deux cotes et
montants opposes atomiques. Si un cote est pointe, la paire suivante est preservee.

Le curseur confirmedThrough empeche une confirmation tardive plus ancienne de
remplacer le montant deja confirme sur une occurrence plus recente. Une edition
manuelle d'une occurrence deja confirmee garde le parcours isole/futures de 0.5.0.
Schema 6 pour confirmations ; archives schema 5 : occurrences non pointees de
numero >0 deviennent en attente, un pointage sur un cote confirme la paire.
Curseur derive des confirmations existantes a la migration, pas des libelles.

Preuves Palm (inspection statique, pas de certification du parcours exact) :
TransForm.m 925-934 sauvegarde le montant edite puis execute les repetitions ;
Transaction.m 3020-3045 copie le modele dans la suivante. Cette structure permet
de reprendre le montant du modele sans modifier les anciennes ecritures.
https://github.com/maxatome/ma-tirelire/blob/master/TransForm.m#L925
https://github.com/maxatome/ma-tirelire/blob/master/Transaction.m#L3020

Essais : date francaise par calendrier, limite unique, annulation, rattrapage de
plusieurs echeances, ajustement de la premiere, protection historique, pointage
independant, virement, confirmation en ordre inverse et sauvegarde/restauration.

# Dates, virements et periodicite — lots de portage

Verification du 10 novembre 2026. Version Android courante : 0.3.4-dev.
Audit statique du depot historique maxatome/ma-tirelire, branche master.
Aucun essai sur Palm ou emulateur ; les adaptations Android sont indiquees.

## Souvenirs confirmes dans les sources

- Date modifiable : obj.rcp lignes 528-537 declare un selecteur de date et
  des boutons precedent/suivant. TransForm.m lignes 3050-3054 traite sa selection.
  Les lignes 1058-1070 montrent la date du jour et une option de conservation
  de la derniere date. obj-fr.rcp ligne 348 nomme cette option.
- Virement : obj-fr.rcp ligne 197 et TransForm.m initXferAccountsListSelected
  montrent le choix du compte lie. Transaction.h lignes 515-540 documente deux
  operations liees, date/heure identiques, montants opposes avec conversion si
  necessaire, pointage et numero de releve independants de chaque cote.
- Periodicite : obj-fr.rcp lignes 203-222 enumere semaine, quinzaine, mois,
  fin de mois, 2/3/4/6 mois, an et deux ans, avec fin par date, nombre ou aucune.
  Transaction.m updateRepeatOfId et computeAllRepeats implementent le calcul
  des occurrences ; les preferences historiques comprennent un mode automatique.

Sources :
- https://github.com/maxatome/ma-tirelire/blob/master/obj.rcp#L528
- https://github.com/maxatome/ma-tirelire/blob/master/TransForm.m#L3050
- https://github.com/maxatome/ma-tirelire/blob/master/Transaction.h#L515
- https://github.com/maxatome/ma-tirelire/blob/master/obj-fr.rcp#L197
- https://github.com/maxatome/ma-tirelire/blob/master/Transaction.m#L2934

## Versions retenues, sans date calendaire promise

| Version | Perimetre | Verification avant livraison |
|---|---|---|
| 0.3.5-dev | Date d'operation dans la saisie et la modification ; aujourd'hui au debut d'une nouvelle saisie, choix manuel possible, date conservee dans le brouillon | Date anterieure, rotation, verrouillage, sauvegarde/restauration et reprise des anciennes dates sans conversion ambigue |
| 0.4.0-dev | Virement interne EUR, compte source et destinataire distincts ; une saisie cree deux ecritures opposees liees | Creation atomique, consolidation inchangee, edition coherente depuis chaque cote, suppression de la paire apres confirmation, pointage independant, sauvegarde/restauration |
| 0.5.0-dev | Periodicite depuis saisie/edition ; frequences historiques et limites par date/nombre/aucune ; generation des echeances dues au deverrouillage | Fin de mois, 29 fevrier, rattrapage, absence de doublons, nouvelles occurrences non pointees, arret/modification de serie, restauration, virements periodiques atomiques |

Ces fonctions sont absentes de la version Android 0.3.4-dev. Un moyen de paiement
nomme Virement ne constitue pas un virement lie et ne cree pas de contrepartie.
Les virements et repetitions etaient deja reserves aux lots 0.4 et 0.5 ; ce
document rend leur premier lot explicite. Les ventilations, devises et autres
fonctions de ces familles seront livrees dans des sous-versions ulterieures.

## Regles d'implementation

La date civile sera stockee independamment de sa presentation locale. La date
d'operation est distincte de la date de saisie et de la future date de valeur.
La migration devra conserver les libelles historiques non interpretables sans
deviner une date a partir de la seule langue actuelle du telephone. Un brouillon
commence avant minuit garde la date proposee au debut jusqu'a modification.

Un virement cree ou modifie ses deux cotes dans une seule transaction de stockage.
Le destinataire est un compte du meme coffre. Pas de virement vers lui-meme.
Modifier le montant ou la date conserve la coherence de la paire ; les drapeaux
de pointage restent independants. La suppression explique qu'elle concerne les
deux comptes. Conversion entre devises : lot ulterieur, pas dans 0.4.0-dev.

La generation periodique a la prochaine ouverture deverrouillee est une adaptation
Android au coffre chiffre : aucun dechiffrement autonome quand il est verrouille.
La session ouverte doit egalement prendre en compte les nouvelles echeances dues.
Une occurrence possede une identite stable pour eviter les doublons apres reprise
ou restauration. Les series ne doivent pas heriter du pointage de leur modele.
L'interface distinguera modifier une occurrence et modifier les suivantes, en
preservant l'historique. Projection des echeances futures et rappels Android sont
des sous-lots separes ; ils ne doivent pas etre annonces comme disponibles en
meme temps que la generation minimale sans validation propre.

Les validations detaillees de periodicite restent dans operations-periodiques.md.
La version courante reste disponible pour les tests ; ce changement est documentaire
et ne declenche aucune compilation ni installation sur le telephone.

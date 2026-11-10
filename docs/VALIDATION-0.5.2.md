# Validation 0.5.2-dev

La regle 0.5.1 de propagation aux occurrences deja generees est remplacee : aucune
autre ecriture du compte ne change, confirmee ou non, pointee ou non. Seule
l'operation editee (ou sa paire de virement) et, au choix, le modele futur changent.
Le choix se fait dans un dialogue au moment de sauvegarder ; la coche disparait.
Annuler le dialogue ne sauvegarde rien. Confirmer ne pointe jamais l'ecriture.

La fiche charge les limites de serie : pour une serie existante, nombre de
nouvelles occurrences restantes, excluant les ecritures deja enregistrees. Vide
sans limite, zero termine. Une date ou un nombre, pas les deux. A la creation,
le nombre total incluant la premiere ecriture reste utilise comme avant.

Recalage de calendrier : ancrage sur la date editee et la nouvelle frequence.
La premiere nouvelle echeance est strictement apres aujourd'hui, la derniere
date enregistree/consommee et la date editee. Le dialogue la montre. Pas de
rattrapage retroactif lors d'une modification de calendrier. Le curseur global
ne recule pas ; scheduleBase/scheduleOffset transposent la nouvelle cadence.
generatedThrough garde la borne des dates consommees, meme apres suppression.
Schema 7 : anciens schemas 1 a 6 lisibles, aucune retrogradation autorisee.

Parametres futurs : date/jour, frequence, montant, categorie, libelle, moyen,
limites, compte d'une operation simple et contrepartie en cas de conversion.
Les comptes/sens/moyen Virement d'une paire existante restent coherents et fixes.
Pour arreter une serie choisir Jamais avec le perimetre serie. Une serie arretee
manuellement reste arretee apres modification ; prolonger une limite epuisee
peut la reactiver. Le pointage reste independant de chaque cote du virement.

Generation suspendue pendant l'edition pour que le perimetre reste stable.
Tests : mensualite 5 vers 6, hebdo lundi vers mercredi, mensuel vers hebdo,
historique en attente preserve, limites restantes, fin de mois, double lancement,
suppression et restauration sans duplication ni recul du curseur.

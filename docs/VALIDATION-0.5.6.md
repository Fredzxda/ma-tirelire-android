# Validation 0.5.6-dev

Dialogue de portee uniquement pour changements effectifs de champs partages :
montant normalise, sens, date, compte, libelle, categorie, moyen et cible.
Simple action de statut sans popup, reglage serie seul garde sa sauvegarde directe.

Parcours choisi par Frederic : Prevision -> A confirmer -> Confirmee, deux
actions distinctes dans la fiche. readyToConfirm persiste l'etat intermediaire
avant echeance ; au jour de l'echeance les previsions passent automatiquement
A confirmer comme avant. amountConfirmed ne passe a true que sur la seconde
action. Aucun pointage implicite ni modification des autres occurrences.
Paire de virement : statuts des deux cotes mis a jour atomiquement, pointages
conserves independamment. Curseur de confirmation non avance a la premiere action.

Schema 9 : readyToConfirm. Archives precedentes : false par defaut, aucune
requalification des confirmations existantes. Rapports : previsions futures
non confirmees toujours exclues, meme preparees A confirmer avant echeance.

Essais : prevision preparee sans popup, confirmation sans popup, retour apres
verrouillage entre actions, virement, pointages et historique intacts, vraie
modification du montant/date/libelle avec popup et parametrage serie sans popup.

# Ventilation et anticipation — 0.7.13-dev

## Ventiler une opération

Une opération ordinaire peut être répartie en lignes comportant une catégorie,
une description et un montant positif. Le signe (dépense ou recette) est celui
de l’opération. La somme doit correspondre exactement à son montant dans la
devise du compte. Aucun arrondi silencieux ne corrige une différence.

Pour une saisie en devise étrangère, la ventilation porte sur le montant converti
dans la devise du compte. Une modification du montant exige donc de réajuster
la ventilation avant enregistrement. Les lignes ne créent pas des opérations
supplémentaires et ne doublent pas le solde.

Un virement interne lié ne peut pas être ventilé. Il faut d’abord retirer
explicitement la ventilation avant de transformer une opération en virement.
La ventilation est conservée dans le coffre et les sauvegardes chiffrées.

Les séries recopient la ventilation de leur modèle. Une modification appliquée
à la série ne modifie pas les autres opérations déjà enregistrées.

## Réduire l’anticipation

Demande validée par Frédéric : retirer les prévisions futures non confirmées et
non pointées qui dépassent la nouvelle limite. Les opérations confirmées restent
présentes même sans pointage. Les occurrences déjà préparées (« À confirmer »)
sont également conservées pour préserver cette action de l’utilisateur.

Les prévisions retirées de la liste et des soldes restent conservées de manière
chiffrée pour revenir sans doublon si l’horizon augmente ou à leur échéance.
Les suppressions manuelles ne sont pas annulées. Une prévision déjà générée
conserve ses données initiales même si le modèle de la série est modifié ensuite,
comme les autres écritures enregistrées. Une paire de virement est
traitée ensemble ; une contrepartie confirmée ou pointée protège la paire.

## Essais manuels sur données fictives

1. Créer une dépense de 50 et la ventiler en 30 et 20, puis la rouvrir.
2. Remplacer 20 par 19 : l’enregistrement doit être refusé avec une indication
   permettant de corriger la différence. Revenir à 20 et enregistrer.
3. Vérifier les catégories hiérarchiques, la rotation et l’accès à la dernière
   ligne en paysage avec le clavier ouvert.
4. Ventiler une série ; vérifier une prochaine occurrence puis modifier une
   seule occurrence : les autres écritures doivent rester inchangées.
5. Passer de 30 jours à la fin du mois puis revenir à 30 jours : les prévisions
   hors limite disparaissent puis reviennent une seule fois. Préparer/confirmer
   ou pointer une occurrence avant réduction pour vérifier sa conservation.
6. Sauvegarder, fermer le coffre et restaurer sur des données de test ; vérifier
   ventilations, soldes et retour des prévisions différées.

Les statistiques par catégorie font partie d’un lot ultérieur ; leur calcul
devra utiliser les lignes de ventilation plutôt que compter deux fois le total.

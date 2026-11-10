# Exigence conservée — opérations périodiques

Identifiant : REC-001. Rappel utilisateur du 8 novembre 2026.
Statut : exigence fonctionnelle retenue pour le portage ; non implémentée dans
0.3.4-dev. Premier lot retenu : 0.5.0-dev, après les dates modifiables en
0.3.5-dev et les virements internes en 0.4.0-dev.

## Comportement attendu

Depuis la saisie ou l'édition d'une opération, l'utilisateur doit pouvoir indiquer
qu'elle est périodique et définir sa fréquence. L'application doit ensuite faire
apparaître automatiquement les occurrences suivantes à l'échéance prévue, sans
ressaisie. Ce fonctionnement fait partie de la cible de parité Palm.

La version Palm contient des répétitions hebdomadaires, par quinzaine, mensuelles,
fin de mois, tous les 2/3/4/6 mois, annuelles et tous les deux ans ; limites par
date, nombre ou sans fin ; calcul des répétitions et vue des échéances futures.
Ce constat vient de l'inspection des sources, pas d'un essai sur Palm.

Sources :
- https://github.com/maxatome/ma-tirelire/blob/master/Transaction.h
- https://github.com/maxatome/ma-tirelire/blob/master/Transaction.m
- https://github.com/maxatome/ma-tirelire/blob/master/obj-en.rcp#L200

## Règles à spécifier avant implémentation

- Différence entre modèle périodique, prévision visible et écriture générée.
- Première date, fréquence, échéance suivante et fin éventuelle de la série.
- Conservation des champs : compte, montant/signe, catégorie, moyen, description,
  éventuelles ventilations/virements et règles de date de valeur.
- Génération à échéance et rattrapage si l'application n'a pas été ouverte pendant
  plusieurs périodes ; parcours compatible avec un coffre verrouillé. Le besoin
  n'impose pas à ce stade un déchiffrement en tâche de fond ni une alarme exacte.
- Garantie d'une seule écriture par occurrence, même après relancement,
  interruption, restauration ou recalcul ; génération atomique.
- Une nouvelle occurrence ne doit pas hériter automatiquement du pointage ou du
  numéro de relevé de l'occurrence précédente. Vérifier les règles Palm des autres champs.
- Édition/suppression : une occurrence, les suivantes ou toute la série ; préserver
  les écritures historiques et ne pas annuler silencieusement leur pointage.
- Dates du 29/30/31, fin de mois, années bissextiles, changement de fuseau et d'heure.
- Interaction entre modifications manuelles d'une occurrence et génération future.
- Effet des sauvegardes/restaurations et des changements de calendrier de la série.

## Scénarios de validation prévus

1. Loyer mensuel : enregistrer une fois, constater la prochaine échéance correcte.
2. Ouvrir plusieurs fois le même jour : aucune duplication.
3. Ne pas ouvrir pendant trois échéances : rattrapage suivant la règle explicitement
   retenue, sans création multiple des occurrences déjà enregistrées.
4. Série du 31 et règle fin de mois : février puis mars restent cohérents.
5. Nouvelle occurrence non pointée ; solde total et prévision selon leurs périmètres.
6. Fin par date/nombre, arrêt et modification de série respectés.
7. Restaurer une sauvegarde puis recalculer : pas de doublon d'occurrence.

Ce document est une exigence suivie, pas l'annonce d'une fonction disponible.

Précision du 10 novembre 2026 : génération des échéances dues au déverrouillage
et pendant la session ouverte, sans déchiffrement en arrière-plan verrouillé.
Projection future et rappels : sous-lots séparés. Voir [le plan détaillé](plan-saisie-2026-11-10.md).

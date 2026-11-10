# Exigence conservée — opérations périodiques

Identifiant : REC-001. Rappel utilisateur du 8 novembre 2026.
Statut : premier lot implémenté dans 0.5.0-dev, après les dates modifiables et
les virements internes. Validation et limites : [VALIDATION-0.5.0.md](VALIDATION-0.5.0.md).

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

## Périmètre livré en 0.5.0-dev

Fréquences historiques et fin par date ou nombre total, génération à échéance
déverrouillée, rattrapage par lots, arret/reprise, édition isolée ou modèle futur,
et virements périodiques. La modification de fréquence/fin/début d’une série
existante, la projection future et les rappels restent des sous-lots ultérieurs.
Les tests unitaires ne remplacent pas la validation manuelle sur téléphone.

## Nouvelle règle de gestion en 0.5.2-dev

Le dialogue à l’enregistrement remplace la coche : opération seule ou modèle
futur. Aucune autre écriture déjà enregistrée ne change, même non confirmée.
Fréquence, jour, limites, montant et références sont modifiables pour les
prochaines générations. Cette version remplace donc la limitation de 0.5.0
sur le changement de calendrier et la propagation de 0.5.1. Projection future
et rappels restent différés. Voir [VALIDATION-0.5.2.md](VALIDATION-0.5.2.md).

## Échéances à venir en 0.5.4-dev

Première vue de projection par compte sur 30/90/365 jours, en lecture seule,
limitée aux séries actives et aux occurrences non encore enregistrées après
aujourd’hui. Les soldes prévisionnels globaux et les rappels restent différés.
Voir [VALIDATION-0.5.4.md](VALIDATION-0.5.4.md).

## Anticipation et états en 0.5.5-dev

Réglage chiffré : à échéance par défaut, fin du mois civil, 15 ou 30 jours.
Les occurrences anticipées sont enregistrées, incluses dans le total et jamais
pointées automatiquement. Prévision / À confirmer / Confirmée sont distincts
du pointage. Réduire l’avance conserve les écritures et curseurs existants.
La projection distincte exclut ces occurrences déjà générées. Voir
[VALIDATION-0.5.5.md](VALIDATION-0.5.5.md) pour les effets sur rapports et soldes.

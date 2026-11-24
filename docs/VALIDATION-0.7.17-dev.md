# Validation 0.7.17-dev — 24 novembre 2026

## Fonctionnement

Réglages → Opérations périodiques → Modifier la prochaine échéance. Un calendrier
sélectionne la prochaine date à générer. Les suivantes suivent la même fréquence.
Une série arrêtée reste arrêtée. Les limites de fin et de nombre sont conservées.
L’anticipation habituelle s’applique : si la date choisie entre dans son horizon,
l’opération est générée et la gestion des séries affiche la suivante à générer.

Les opérations enregistrées, y compris les prévisions différées, ne sont pas
déplacées. La nouvelle date est au plus tôt aujourd’hui, et strictement après
la dernière opération enregistrée de la série. Une date dépassant la fin est
refusée. Une série au nombre épuisé ou sur compte archivé ne peut être replanifiée.
Il faut modifier les limites ou désarchiver le compte avant de réessayer.

Le curseur d’occurrence ne recule pas ; la série est réancrée à la nouvelle date.
Le format du coffre reste inchangé. Modifier ensuite le seul montant ou une
autre information non calendaire ne doit pas annuler cette date explicite.

## Contrôles

- 197 tests unitaires réussis ; compilation de l’application et des tests, lint réussis.
- Six nouveaux tests : dates mensuelles suivantes, prévisions différées et
  refus atomique, série arrêtée/limites, paires hebdomadaires/comptes archivés,
  conservation de la date après une modification de montant, absence de décalage
  des fins de mois lorsque la prochaine date est validée sans changement.
- Test appareil ajouté : sélection et confirmation dans le calendrier natif,
  puis annulation sans modification, activité isolée avec données de test.
- 27 tests instrumentés réussis sur ASUS ; installation 0.7.17-dev, versionCode 47
  et relancement vérifiés. CI GitHub non observée.

## Essais manuels

1. Choisir une nouvelle prochaine date dans la gestion des séries.
2. Vérifier la date affichée et les projections dans Échéances à venir.
3. Vérifier qu’aucune écriture du compte n’a été déplacée.
4. Vérifier une série arrêtée : sa reprise doit rester une action explicite.

Les tests isolés n’utilisent pas le coffre personnel et ne couvrent pas toutes
les orientations ou tous les modèles de téléphone.

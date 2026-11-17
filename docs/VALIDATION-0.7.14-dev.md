# Validation 0.7.14-dev — 17 novembre 2026

Version compilée : 0.7.14-dev, versionCode 44.

## Comportement

- Catégories ventilées réunies pour la lecture, sans créer une catégorie de
  catalogue concaténée. Les anciennes données restent inchangées.
- La catégorie principale technique conservée dans les formats existants est
  cachée lorsque la ventilation est active ; les lignes font référence.
- Suggestions de descriptions locales au coffre, limitées au compte et au sens
  de l’opération. Sélection, fin du champ ou correspondance exacte à l’enregistrement ; aucun montant, date ou virement copié.
- Reprise de la catégorie et du moyen seulement si leurs champs sont vides.
  Les références masquées/non utilisables ne sont pas reprises. Une ancienne
  ventilation ne sert pas à proposer une catégorie principale trompeuse.
- La dernière saisie conservée est choisie par identifiant décroissant ; la
  date comptable peut avoir été saisie rétroactivement. Pas de nouvel historique
  extérieur au coffre, pas d’accès réseau.

## Séries supprimées manuellement

Le champ `next` garde le prochain indice à générer et `generatedThrough` la
dernière date consommée. Supprimer une occurrence ne recule pas ce curseur :
elle ne revient pas lors d’un changement d’anticipation. C’est distinct des
prévisions différées depuis la 0.7.13-dev, qui sont conservées chiffrées pour
revenir à leur échéance ou si l’horizon augmente. Aucun rattrapage automatique
des suppressions manuelles ni modification du coffre personnel n’est effectué.

## Vérifications

- Compilation de l’application et de son APK de tests réussie.
- 186 tests unitaires réussis, aucun échec, aucune erreur, aucun test ignoré.
- Lint réussi ; avertissement existant de dépréciation des API Compose de tests.
- Métadonnées APK vérifiées : 0.7.14-dev, versionCode 44.
- Revue indépendante des changements effectuée, aucun blocage identifié.
- Trois nouveaux tests instrumentés : suggestions et préservation des choix en
  portrait/paysage, résumé des catégories ventilées. Ils portent sur les composants
  dans une activité fictive ; pas sur une rotation dans la fiche complète.

Installation, lancement et tests instrumentés non effectués : ASUS configuré
indisponible dans ADB. Aucune validation sur appareil revendiquée pour cet APK.
Le harnais `tools/play.ps1` reste à exécuter après reconnexion. Le coffre personnel
n’a pas été utilisé. CI GitHub non observée lors de cette validation locale.

## Essais manuels

1. Ventiler une dépense entre deux catégories : vérifier la liste puis la fiche.
2. Commencer une dépense sur le même compte et choisir une description connue :
   vérifier le remplissage des références vides, puis enregistrer.
3. Recommencer en choisissant d’abord une catégorie/un moyen différents :
   sélectionner la suggestion ne doit pas écraser ces choix.
4. Vérifier les suggestions en portrait et paysage, clavier ouvert, puis revenir
   après verrouillage : le brouillon reste sous la protection du coffre.

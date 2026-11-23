# Validation 0.7.16-dev — 23 novembre 2026

Icônes de catégories : 64 pictogrammes vectoriels localisés FR/EN, choix dans
la création et les réglages, Aucune ou héritage explicite, affichage avant le
statut avec trois icônes distinctes puis +N. Masquage global dans la gestion des
catégories. Les noms restent affichés et les virements internes n’utilisent pas
de catégorie technique pour leurs pictogrammes.

## Données

Format 17 uniquement si une icône, un héritage ou le masquage global est utilisé.
Lecture des anciens formats sans affectation automatique. Choix conservés dans
le coffre et les sauvegardes chiffrées ; identifiants inconnus et options
contradictoires rejetés. Aucun changement de solde ni de format des opérations.
Avant un retour à une version antérieure, conserver une sauvegarde compatible.

## Contrôles

- 191 tests unitaires réussis, compilation et lint réussis.
- Ressources des 64 vecteurs compilées ; catalogue de référence conservé.
- 26 tests instrumentés réussis sur ASUS, activité isolée et données de test.
  Installation 0.7.16-dev, versionCode 46 vérifiée ; application relancée.
  Les nouveaux tests couvrent enregistrement explicite, annulation/héritage,
  défilement en paysage, ventilation +N et masquage global.
- Les tests de composants isolés ne couvrent pas tous les parcours du coffre
  personnel, toutes les tailles de texte et toutes les rotations.
- CI GitHub non observée pendant cette validation locale.

## Essais manuels

1. Réglages, Catégories et moyens de paiement : choisir une icône puis Enregistrer.
2. Vérifier l’icône avant le statut sur une carte et sur une opération hors série.
3. Catégorie fille : Utiliser celle du parent ; puis Aucune pour arrêter cet héritage.
4. Ventilation de quatre catégories avec icônes différentes : trois pictogrammes +1.
5. Désactiver l’affichage global ; fermer/rouvrir le coffre et vérifier la préférence.
6. Rechercher une icône et atteindre la dernière en paysage ; annuler sans enregistrer.

La stratégie de validation proportionnée est décrite dans workflow-validation.md.

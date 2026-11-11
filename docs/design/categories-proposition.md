# Catégories — proposition visuelle du 11 novembre 2026

Statut : proposition à valider, application toujours en 0.7.6-dev.

[Ouvrir la maquette interactive](categories-proposition.html). Données fictives,
aucune dépendance réseau et aucun stockage persistant. Les créations et modifications
de démonstration disparaissent au rechargement.

## Arbre commun à la saisie et aux réglages

- Retraits, traits de branche et flèches montrent la hiérarchie, y compris sur
  plus de deux niveaux. Flèche et nom ont deux cibles tactiles distinctes.
- La flèche replie/déplie ; le nom sélectionne, même sur un parent. Sélection :
  fond teinté et coche, sans dépendre uniquement de la couleur.
- Le champ de catégorie affiche le chemin complet : `Logement › Charges › Électricité`.
- La recherche retrouve un enfant et rend son ascendance visible. En sortant
  de la recherche, les branches retrouvent leur état précédent.
- Les catégories masquées restent identifiables dans les réglages par un badge ;
  les restrictions de type/compte et la conservation des références historiques
  restent celles de la 0.7.6. Le masquage du parent ne masque pas ses enfants
  implicitement : son nom pourra servir de contexte non sélectionnable si nécessaire.

## Création depuis les réglages

Bouton `＋ Nouvelle catégorie`, également disponible en pied du sélecteur de saisie.
Formulaire : nom, parent facultatif (catégorie sélectionnée proposée), Créer/Annuler.
`Aucune` crée une catégorie principale. La catégorie créée devient sélectionnée
et sa branche est dépliée. Les options avancées sont accessibles ensuite.
Prévoir de même `＋ Nouveau moyen` dans l'onglet Moyens, sans hiérarchie.

## Portrait, paysage et accessibilité

Dans les réglages, arbre puis propriétés en portrait étroit ; côte à côte quand
la largeur le permet. La liste défile dans sa zone, sans ajouter de commande à
l'en-tête Opérations. En paysage, la saisie laisse davantage de largeur au sélecteur.
Chemins longs avec retour à la ligne ; cibles de 48 dp, noms accessibles comprenant
le chemin et état de dépliement annoncé. Pas de limite fonctionnelle inventée
au nombre de niveaux ; vérifier la lisibilité des cas profonds.

La maquette illustre l'arbre, la recherche, les sélections, la création et le
renommage/reparentage. Les propriétés avancées sont illustratives. Les restrictions
complexes, validations comptables et persistance restent à intégrer dans Android
après validation du visuel ; aucune compilation ni installation dans ce lot.

# Comptes et séries externes — 0.7.12-dev

## Séries de virements après archivage/suppression

Quand un compte d’un virement interne est archivé ou supprimé, le modèle futur
devient un virement externe sur l’autre compte actif. Il conserve son identifiant,
son état actif/suspendu, la périodicité, le curseur, les limites et le dernier
montant du modèle dans la devise du compte conservé. Il ne reprend pas le montant
d’une ancienne occurrence. Les taux locaux courants ne recalculent pas ce montant.

Les opérations déjà enregistrées ne sont pas modifiées par l’archivage.
Lors d’une suppression, les contreparties des autres comptes restent présentes
et sont dissociées, comme dans la 0.7.11. Montants, pointages et états de confirmation
restent inchangés. Les nouvelles échéances sont créées seulement sur le compte
actif conservé, sans lien interne ni doublon.

Les séries ordinaires propres au compte retiré restent suspendues. Si aucun
compte concerné n’est actif, aucune série ne doit être relancée implicitement.
Une série volontairement suspendue reste suspendue. Désarchiver un compte ne
recrée pas de lien ou de seconde série et ne reprend pas les séries arrêtées.

## Anciens modèles de la 0.7.11

La lecture d’une ancienne archive/coffre au schéma 14 convertit les modèles
internes visant un seul compte archivé vers le compte actif restant. Leur état
suspendu est conservé : il est impossible de distinguer une pause volontaire
d’un arrêt automatique effectué par la 0.7.11. Reprendre la série manuellement
dans Opérations périodiques si souhaité. Les modèles déjà externes issus d’une
ancienne suppression restent également dans leur état initial.

Cette adaptation est idempotente, sans modification d’opérations ni nouveau
format de sauvegarde. Le schéma 14 et la lecture des anciens schémas sont conservés.

## Présentation

- Bouton Désarchiver directement dans la section Comptes archivés, en plus de
  la fiche du compte. Un changement de statut conserve le brouillon de configuration.
- Le conteneur de glissement est découpé à la forme arrondie des cartes : le
  fond ne produit plus de coins carrés au repos.
- Fond vert pour archiver, rouge soutenu pour supprimer ; texte et icônes blancs.
  Icône de boîte d’archive et corbeille vectorielle, sans caractères de substitution.
- La confirmation reste obligatoire après le geste, annulation sans mutation.

## Essais utilisateur

Créer un virement périodique entre deux comptes de test, en variant le compte
source/cible et les devises. Archiver l’un, puis vérifier que la prochaine échéance
apparaît uniquement sur l’autre compte, au montant courant du modèle. Désarchiver :
aucun doublon ni rétablissement automatique du lien. Essayer aussi une suppression.
Vérifier les anciens montants, les états de confirmation et les pointages.
Contrôler visuellement les coins, fonds et icônes en portrait et paysage.

La refonte complète de la vue Réglages/Configuration est requise avant 1.0.0 ;
elle est planifiée séparément et ne fait pas partie de cette correction.

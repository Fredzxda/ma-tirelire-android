# Validation 0.3.0-dev

Premier lot 0.3 : edition et suppression des operations. Comptes personnalisables,
dates modifiables et pointage guide restent dans les prochains lots.
Le format de coffre/archive est conserve ; editingId est un champ facultatif du
brouillon. Les archives 0.2 restent lisibles. Ne pas retrograder avec une edition
en cours : une ancienne version ne connait pas la cible du brouillon.

## Essais sur donnees fictives

1. Ouvrir : authentification Android directement, puis code supplementaire si choisi.
2. Annuler : pas de relance en boucle ; le bouton permet de reessayer.
3. Passer en arriere-plan puis revenir : authentification automatique de nouveau.
4. Rotation pendant/suite a authentification : pas de demandes multiples.
5. Modifier une depense pointee : montant, libelle, categorie, moyen et compte.
   Date, identite et pointage conserves ; verifier les deux soldes des deux comptes.
6. Tourner l'ecran et verrouiller avec un brouillon de modification : apres
   deverrouillage, retrouver le brouillon ; enregistrer sans creer de doublon.
7. Supprimer : annulation sans effet, confirmation retire la ligne et recalcule.
8. Sauvegarder, modifier/supprimer une operation, restaurer : retrouver l'etat
   sauvegarde. Retour du selecteur avec authentification automatique.

Automatise : politique anti-boucle, edition FR/US, preservation date/pointage,
soldes, suppression, brouillons et compatibilite des archives historiques.
Les tests sur appareil utilisent un stockage isole et ne deverrouillent pas le
coffre utilisateur. Le parcours biometrie/SAF reel reste a valider manuellement.

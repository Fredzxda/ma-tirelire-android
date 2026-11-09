# Validation — 0.1.4-dev

9 novembre 2026. Branche `dev/portrait-0.1.4`, base `ee4c922`.

En saisie portrait/fenêtre étroite, comptes sur une seule ligne : boutons à la
largeur du libellé, sans retour à la ligne, défilement horizontal en cas de débordement.
Le paysage conserve sa colonne de comptes à gauche. Aucun changement métier.

Validation via le harnais `tools/play.ps1` réussie : compilation, sept tests du
modèle et de saisie monétaire, lint, installation sur ASUS et lancement confirmé.
Version installée vérifiée : 0.1.4-dev. Ces tests ne mesurent pas le rendu de l'écran.

À vérifier sur téléphone : comptes côte à côte en portrait, sélection fonctionnelle,
défilement avec gros caractères ou libellés longs, puis rotation vers le paysage.
Le brouillon doit rester identique. Aucun essai visuel automatisé dans cette étape.
Le fichier local Gradle non suivi est préservé, hors du commit.

Toujours une démonstration sans données réelles ni stockage persistant.

# Validation 0.2.5-dev

Correction du parcours local de sauvegarde et restauration : sélecteur unique,
confirmation de sélection et résultat dans un dialogue visible indépendamment
du défilement. L’authentification après le sélecteur externe reste obligatoire.
La sélection chiffrée est conservée pendant le verrouillage ; les secrets saisis
ne sont pas conservés en arrière-plan.

## Vérification manuelle sur données fictives

1. Choisir un dossier local, déverrouiller au retour et vérifier la confirmation.
2. Configurer le mot de passe de sauvegarde et vérifier la confirmation.
3. Créer une sauvegarde manuelle et vérifier la confirmation.
4. Ajouter une nouvelle dépense fictive reconnaissable ; noter le solde.
5. Choisir la sauvegarde créée avant cet ajout. Après déverrouillage, vérifier
   un seul bouton « Choisir une autre sauvegarde locale » et le formulaire.
6. Essayer un mauvais mot de passe : erreur visible, comptes inchangés.
7. Ressaisir le bon mot de passe et accepter la restauration : confirmation
   visible, dépense ajoutée absente et ancien solde retrouvé.

L’édition des opérations est prévue ultérieurement ; le test repose sur un ajout.
Une sauvegarde de sécurité de l’état courant est obligatoire avant remplacement.
Les tests automatisés ne valident pas visuellement ce parcours SAF/biométrie.
# Validation 0.5.3-dev

Detection des champs communs normalisee : montant selon locale (sans tenir compte
de la notation 10/10,00), signe, date, compte, libelle, categorie, moyen, cible.
Si seuls frequence/fin/nombre restant changent, sauvegarde directe de la serie.
Ne pas confirmer l'occurrence, modifier une ligne ou recopier ses anciens champs
dans le modele futur. Un changement de limite seul conserve le calendrier et le
curseur. Un changement de frequence recale uniquement les generations futures.

Modification mixte ou champs d'operation : dialogue de portee conserve. Le texte,
la prochaine date et les trois boutons sont dans une meme colonne defilante,
avec espacement fixe, sans les emplacements confirm/dismiss de Material AlertDialog.
Clavier masque avant ouverture et FLAG_SECURE conserve.

Essais : fin seule, nombre seul, frequence seule, historique intact et montant du
modele different du montant historique, modification mixte avec popup, annulation,
texte lisible et boutons reguliers en portrait/paysage et grande police.

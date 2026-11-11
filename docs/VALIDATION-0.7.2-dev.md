# Validation 0.7.2-dev — periodes et normalisation distincte

## Compte

Mois courant par defaut, fleches precedent/suivant, Ce mois, Tout l historique
et Dates a verifier si des dates restent illisibles. Les anciennes dates FR/US/UK
reconnues appartiennent au bon mois sans conversion silencieuse du stockage.
Le filtre garde le tri selectionne et ne modifie aucune ecriture ni solde.
Soldes total/pointe du compte entier ; recherche sur tout l historique du compte,
independamment de la periode. Rapprochement et echeances restent globaux au compte.
Une edition changeant la date peut faire sortir l ecriture du mois affiche ; elle
reste accessible via son nouveau mois, Tout l historique et la recherche.

Periode en memoire du ViewModel, conservee a la rotation et partagee entre comptes
pour la session ; remise au mois courant apres verrouillage. Ce mois suit un
changement de mois en session via la date metier actualisee ; un mois explicitement
selectionne reste fixe. Aucun nouveau format de coffre ni limite relevee : liste
paresseuse, snapshot toujours limite a 10 000 ecritures, pas de pagination SQL ici.

## Normaliser

Demande confirmee par Frederic : sauvegarde puis normalisation separees.
Normaliser ne cree plus d archive. Si une conversion ou un marqueur de migration
reste a enregistrer, il exige un checkpoint de sauvegarde du contenu actuel :
empreinte du snapshot et de l archive, URI stockes uniquement dans la configuration
chiffree apres succes de l ecriture et de sa relecture. Le fichier existant est
relu et verifie avant migration ; archive absente, alteree, contenu modifie ou
ancien checkpoint absent => demande d une nouvelle sauvegarde via son propre bouton.
Une sauvegarde creee avant 0.7.2 ne porte pas ces empreintes et doit etre refaite
si une normalisation est encore necessaire. Aucun secret ni empreinte en journal.
Une normalisation deja terminee sans nouvelles dates reconnues reste sans effet
et ne demande pas de sauvegarde supplementaire. Les dates ambigues restent signalees.
Verification du snapshot courant avant remplacement atomique toujours conservee.

## Tests et essais

Unitaires : bornes des mois, annee/bissextile, dates historiques, dates illisibles,
mois vide, ordre preserve, recherche complete et comptabilite non modifiees,
reset de session ; refus des empreintes absentes, archive alteree ou snapshot change.
Les tests d empreintes utilisent du contenu synthetique ; ils ne sont pas une
validation manuelle du fournisseur de fichiers sur le coffre personnel.

Essais : defiler et naviguer en portrait/paysage, revenir a Ce mois, afficher tout,
rechercher et modifier hors du mois, verifier soldes et pointages, rotation puis
verrouillage. Pour une migration encore necessaire : sauvegarder, puis Normaliser
sans modification intermediaire, verifier absence d archive supplementaire ; une
modification intermediaire ou un fichier supprime doit empecher la normalisation.

Retour arriere code : 87118c3. Coffre schema 11 toujours compatible avec 0.7.1 ;
ne pas desinstaller et conserver une sauvegarde chiffree avant tout retour.
Validation finale : compilation, 115 tests unitaires et lint reussis. ASUS :
installation 0.7.2-dev verifiee, trois tests SQLCipher/Keystore isoles reussis
et application relancee. Rendu et parcours sur coffre personnel a verifier par
Frederic ; aucune migration ni lecture de ses donnees par le harnais.

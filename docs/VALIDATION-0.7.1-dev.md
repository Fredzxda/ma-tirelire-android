# Validation 0.7.1-dev — migration des dates et raccourcis

## Comportement

Dans Reglages, Sauvegarder et normaliser les dates cree une archive manuelle
chiffree avec le mecanisme existant : fichier local, relecture et comparaison
avant succes. Sans configuration de sauvegarde valide ou en cas d echec, aucune
conversion n est enregistree. Le coffre n est pas migre automatiquement au lancement.

Le plan convertit seulement les dates historiques FR/US/UK avec mois en lettres,
vers ISO AAAA-MM-JJ. Le parseur interne de saisie reste strict ISO. Dates deja ISO
inchangees, dates ambigues/non reconnues conservees, nombre indique dans les
reglages et mention Date a verifier sur les cartes concernees. Correction manuelle
possible via la fiche et son calendrier. Montants, pointages, IDs, paires,
series, brouillon et preferences conserves ; validation complete du snapshot.

L archive est creee avant ecriture, puis le snapshot courant est compare a celui
analyse : une modification intervenue pendant la sauvegarde annule la migration.
Le nouveau snapshot est enregistre par une transaction SQLCipher puis charge en
memoire. Migration versionnee 1, schema 11 uniquement une fois executee. Reexecuter
ne produit pas de modification ni de sauvegarde supplementaire si deja normalise.
Les schemas 1 a 10 restent lisibles. Une restauration ancienne peut etre normalisee
a son tour ; pas de conversion silencieuse durant la restauration.

Raccourcis : popup via version portrait, rail paysage et version des reglages ;
bouton dedie retire, ouverture automatique une fois par version conservee.
Dans le compte, fleche cyclique saisie / recent / ancien, preference chiffree
partagee avec les reglages et la recherche. Affichage mensuel reserve a 0.7.2.

## Tests et essais

Tests unitaires : conversion selective, idempotence, round-trip versionne,
conservation des deux cotes d un virement et de leurs pointages independants,
backup en echec sans ecriture, edition concurrente sans ecrasement,
ordre sauvegarde puis remplacement, rejet d une version de migration inconnue.

A tester par Frederic : toucher les versions dans les deux orientations ;
changer le tri dans le compte et rouvrir apres verrouillage ; normaliser depuis
Reglages avec sauvegardes configurees, verifier le message de fin et l archive
manuelle creee, les soldes, les pointages et les dates localisees ; recommencer
sans changement ; corriger manuellement les dates signalees si necessaire.
Aucune manipulation du coffre personnel par le harnais de tests.

## Retour arriere

Version precedente : 2d1c604 (0.7.0.1-dev). Conserver l archive pre-migration.
Les anciennes applications refusent le schema 11 : ne pas retrograder directement
sur le coffre migre ni desinstaller. Une archive creee avant migration conserve
son ancien schema et sert de point de recuperation avec une version compatible.
Validation realisee : compilation, 107 tests unitaires et lint reussis. ASUS :
installation 0.7.1-dev verifiee, trois tests SQLCipher/Keystore isoles reussis,
application relancee. Migration et rendu sur coffre utilisateur non executes
par le harnais ; essais utilisateur ci-dessus attendus.

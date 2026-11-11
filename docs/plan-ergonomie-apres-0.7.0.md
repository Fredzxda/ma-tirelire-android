# Ajustements apres 0.7.0-dev — 11 novembre 2026

## 0.7.1-dev — raccourcis de presentation

- Retirer le bouton Nouveautes et points a tester des reglages.
- Ouvrir le meme dialogue en touchant le numero de version : en-tete portrait,
  titre du menu paysage et version dans les reglages. Conserver l ouverture
  automatique une fois par nouvelle version apres deverrouillage.
- Ajouter un bouton icone compact dans le compte pour changer le tri.
  Icone distincte selon saisie/date croissante/date decroissante, description
  accessible traduite indiquant le tri actuel et l action suivante. Zone tactile
  de 48 dp. Utiliser le reglage chiffre existant, coherent avec les reglages
  et la recherche ; ne pas ajouter de preference en clair.

## 0.7.2-dev — affichage par periode (proposition Android)

- Mois courant a l ouverture, navigation mois precedent/suivant, retour au mois
  courant et option Tout l historique, sans suppression ni purge.
- Soldes fixes total/pointe du compte entier : ne pas les recalculer sur les
  seules lignes du mois. Nom de periode visible, nombres/dates localises FR/EN.
- Recherche sur tout l historique du compte, clairement indiquee, meme quand
  la liste principale affiche un mois ; resultat toujours editable.
- Ne pas rendre les anciennes dates non reconnues introuvables : acces explicite
  aux dates a verifier et via Tout l historique/recherche. Ne pas deviner les dates.
- Previsions enregistrees visibles dans leur mois ; pointage et confirmation
  independants. Aucun effet sur la generation des series ou la reconciliation.
- Conserver une liste paresseuse : le filtrage mensuel ameliore la navigation,
  mais ne constitue pas une pagination de la base. La limite actuelle de 10 000
  ecritures par snapshot reste un chantier distinct, sans promesse d historique illimite.
- Valider limites des mois/annees, fevrier bissextile, changement de mois pendant
  session, rotation/verrouillage, editions changeant la date ou le compte.

## Verification historique Palm

Audit statique uniquement, branche master du depot maxatome/ma-tirelire.
- TransScrollList.m, getDataForItem, lignes 1890-1903 : parcours via
  DmQueryNextInCategory selon le compte, sans condition mensuelle dans cette methode.
- Meme fichier, SCROLLLIST_GOTO_DATE, lignes 1727-1788 : choix d une date puis
  recherche de la premiere ecriture a cette date ou apres, et positionnement
  en fin de liste si aucune ne correspond.
- obj-fr.rcp, Stats-BetweenList, lignes 468-487 : Mois en cours, Mois dernier,
  annee et Tout sont des choix de periode des statistiques.

Sources :
- https://github.com/maxatome/ma-tirelire/blob/master/TransScrollList.m#L1890
- https://github.com/maxatome/ma-tirelire/blob/master/TransScrollList.m#L1727
- https://github.com/maxatome/ma-tirelire/blob/master/obj-fr.rcp#L468

Conclusion limitee aux passages examines : pas de preuve que la liste principale
etait limitee au mois courant. Le filtre mensuel propose est une adaptation Android.

## Signalement de tri — diagnostic en attente

Les exemples verbaux correspondent au comportement attendu : NEWEST place les
anciennes dates en bas ; OLDEST les recentes en bas. Pas de correction speculative.
TransactionOrder compare des LocalDate ISO ; egalites stables et anciennes dates
non interpretables placees en fin dans les deux sens. Ce dernier cas peut expliquer
une ligne qui semblerait hors ordre ; hypothese non confirmee.
Demande a Frederic : uniquement les trois dernieres dates dans l ordre affiche
et le mode de tri. Aucun libelle, montant, extrait du coffre ou capture requis.
Tests existants verifies par lecture : tri dans les deux sens, egalites, dates
illisibles, conservation du choix via snapshot. Aucun code applicatif modifie ici.

Diagnostic complete : exemple utilisateur 2 nov., 2 nov., 10 nov. en tri recent.
Regression reproduite avec dates ISO et anciennes dates MEDIUM localisees.
Correctif 0.7.0.1-dev : lecture stricte FR/US/UK pour le tri uniquement ;
aucune reecriture du coffre. Raccourcis 0.7.1 et periode 0.7.2 inchanges.

0.7.1-dev realisee : raccourcis version/tri et migration des dates sauvegardee.
L affichage mensuel 0.7.2 reste a venir.

0.7.2-dev realisee : navigation mensuelle, historique et dates a verifier.
Sauvegarde puis normalisation desormais distinctes, checkpoint existant exige
si une conversion reste necessaire. Aucun archivage ni purge automatique.

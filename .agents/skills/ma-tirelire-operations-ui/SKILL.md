---
name: ma-tirelire-operations-ui
description: Concevoir ou faire évoluer la vue Opérations de Ma Tirelire Android en conservant son modèle visuel compact, ses menus temporaires et sa hiérarchie de cartes, en portrait et paysage.
---

# Vue Opérations de Ma Tirelire

Appliquer ce contrat lors d'un changement de présentation de la vue Opérations
ou de l'ajout d'une commande à cette vue. Il ne concerne pas les autres projets.
La référence versionnée se trouve dans le dépôt `C:/Dev/ma-tirelire-android` :
`docs/design/operations-proposition.md` et sa maquette HTML voisine.

Statut initial : organisation générale demandée par Frédéric le 11 novembre 2026 ;
carte et maquette proposées pour validation. La demande courante impose de montrer
la proposition avant de modifier l'application. Une validation ultérieure autorise
l'implémentation correspondante sans redemander cet accord à chaque ajustement.
Actualiser le statut dans la référence quand Frédéric valide ou corrige la proposition.

## Structure à préserver

- En haut, compte sélectionné et soldes total/pointé fixes, suivis d'une seule
  ligne de menu : Tri, Compte, Filtre, Recherche, Fonctions. La liste seule défile.
- Tri : icône qui applique directement le changement ; garder la préférence
  chiffrée existante et une description accessible du tri et de l'action.
- Les quatre autres entrées ouvrent une seule ligne contextuelle, située entre
  le menu et la liste. Une seule ouverte à la fois ; retoucher l'entrée la ferme.
  Défilement horizontal dans cette ligne si nécessaire, sans empiler des rangées.
- Compte : sélecteurs des comptes. Filtre : période, précédent/suivant, mois
  courant, historique et accès aux dates à vérifier. Recherche : champ intégré
  et effacement, sans fenêtre de résultats séparée. Fonctions : rapprochement,
  échéances et futures actions spécialisées ; pas de nouveaux boutons permanents.
- Cliquer hors de la ligne contextuelle la referme, sans effacer les critères
  sélectionnés. Un clic sur une carte ou sur son pointage ferme la ligne et
  exécute l'action ciblée exactement une fois. Ne pas poser un voile qui absorbe
  ce premier clic. Ne pas fermer au focus du clavier, à une interaction dans la
  ligne, à une rotation ou à l'ouverture du clavier logiciel.
- Un compte, un filtre et une recherche restent combinables : période ET mots
  recherchés. « Tout l'historique » étend la recherche à toutes les dates.
  Le texte reste actif quand la ligne est fermée ; montrer un indice discret
  sur Recherche et la période sur Filtre, sans nouvelle barre permanente.
- Les explications générales appartiennent au dialogue Informations : soldes
  globaux, prévisions, pointage indépendant, portée du filtre et de la recherche.
  Aucun paragraphe pédagogique permanent au-dessus de la liste. Les erreurs et
  critères effectivement actifs restent visibles ; ne pas les masquer en aide.

## Portrait et paysage

Portrait : compte et deux soldes côte à côte, menu de 48 dp environ, puis au plus
une ligne contextuelle. Éviter un autre grand en-tête de marque dans cette vue.
Paysage : titre/version dans le rail gauche sur fond bleu foncé `#142D36`, fond
du rail sur toute la hauteur ; compte et soldes dans une ligne compacte.
Garder la même structure de commandes sans réintroduire un grand en-tête au-dessus.
Ne pas imposer une hauteur rigide qui tronque les grands montants, traductions
ou texte Android agrandi ; adapter et mesurer la place laissée à la liste.

## Carte proposée

- Première ligne : statut de récurrence en haut à gauche, date en haut à droite.
  Ne pas inventer un statut pour une écriture hors série.
- Deuxième ligne : description à gauche, montant à droite, alignement supérieur
  explicite. Ne pas centrer verticalement la description sur le bloc montant/pointage.
- Sous la description : catégorie et moyen ; sous le montant : pointage bancaire.
  Lien de virement ou autre métadonnée sur la gauche si utile, sans surcharge.
- Montant aligné à droite ; libellés longs autorisés à revenir à la ligne.
  Statut de série et pointage sont indépendants visuellement et fonctionnellement.
  Cible tactile du pointage d'au moins 48 dp, sémantique de case à cocher ;
  toucher le pointage n'ouvre pas la fiche et toucher la carte l'ouvre.

## Champs de périodicité

Les libellés « Date de fin » et « Occurrences restantes » restent toujours dans
l'encoche du contour, même vides et sans focus. Même composant visuel, style,
position de label, marges et hauteur de contenu. Le texte saisi ne doit pas
déplacer le libellé ni faire grandir un seul des deux cadres. La date continue
d'ouvrir un calendrier ; pas de retour à une saisie textuelle ambiguë.
Si le texte agrandi manque de place, agrandir les deux cadres ensemble ou les
empiler ; ne pas rogner les labels pour conserver artificiellement deux colonnes.

## Vérification d'un changement

Lire la proposition et les corrections validées avant de modifier cette vue.
Vérifier portrait/paysage, menu fermé et chaque menu ouvert, clavier, clic hors
ligne, action de carte et pointage, longs libellés/montants, dates illisibles,
texte Android agrandi. Critères combinés, rotation et verrouillage ne doivent
ni modifier la comptabilité ni exposer des données dans un état sauvegardé en clair.
Ne pas réintroduire la recherche indépendante du filtre de la 0.7.2 : le modèle
demandé ici combine les critères. Les règles de confirmation, de séries, de
virements et de sauvegarde restent celles du dépôt, hors de ce contrat visuel.

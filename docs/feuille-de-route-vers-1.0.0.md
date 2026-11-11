# Feuille de route vers 1.0.0

État au 11 novembre 2026, après validation utilisateur de 0.7.4-dev.
**Découpage proposé : ordre et numéros ajustables, sans dates promises.**
Ce document actualise les lots initiaux du cadrage du 8 novembre ; leur numéro
ne prouve pas leur achèvement. Les fonctions manquantes des anciens lots sont
reprises ici. Chaque lot pourra être fractionné selon sa taille et les essais.

## Déjà disponible

- Coffre local chiffré, authentification Android automatique, code supplémentaire
  facultatif, verrouillage en arrière-plan, aperçu et captures protégés.
- Sauvegardes chiffrées locales manuelles et hebdomadaires dues à l'ouverture,
  restauration et normalisation des dates avec sauvegarde distincte vérifiée.
- Comptes en euros : création, nom, solde initial, soldes total et pointé.
- Saisie, modification, suppression, catégories/moyens simples réutilisables,
  dates, virements internes liés en euros et conversion d'une écriture simple.
- Séries : fréquences, limites, génération/anticipation, prévision/à confirmer/
  confirmée, portée des modifications et préservation des écritures enregistrées.
- Pointage manuel et rapprochement guidé par solde cible, échéances à venir.
- Tri, recherche textuelle, navigation mensuelle/historique, interface adaptative,
  FR/EN et formats suivant la locale du téléphone.

Ces fonctions sont implémentées ; elles restent incluses dans les essais de
non-régression et les validations finales de sécurité/récupération.

## Prochains lots proposés

| Version | Reste à implémenter | Résultat attendu |
|---|---|---|
| 0.7.6-dev | Catégories/moyens avancés et stockage associé | Renommer, organiser, retirer ou remplacer sans casser les références ; catégories hiérarchiques, types et visibilité par compte. Regroupe le socle des historiques longs initialement prévu en 0.7.5 : stockage, chargement, migrations et sauvegardes adaptés. Pas de version technique distincte à tester. |
| 0.7.7-dev | Ventilation des opérations | Répartir une écriture entre plusieurs catégories/descriptions ; somme exacte, édition des sous-opérations et articulation avec les virements. |
| 0.7.8-dev | Devises et virements complémentaires | Devise par compte, taux locaux, montant d'origine et montant du compte, arrondis, transferts entre devises, dissociation d'un lien de virement et consolidation cohérente. |
| 0.7.9-dev | Champs bancaires et chéquiers | Heure, date de valeur, règles associées aux moyens de paiement, numéros de chèque et de relevé, numérotation/changement/épuisement des chéquiers. |
| 0.8.0-dev | Modèles de saisie et macros | Préremplir description, compte, catégorie, moyen, montant/signe et éventuel virement ; options de validation/raccourcis historiques à détailler. |
| 0.8.1-dev | Rappels et alertes | Rappels d'opérations/échéances, seuils de découvert, réglages de notifications ; contenu générique protégeant les informations financières. |
| 0.8.2-dev | Recherche avancée | Période personnalisée, montants, catégories/moyens précis, chèques, relevés, états et combinaisons de critères au-delà de la recherche textuelle actuelle. |
| 0.8.3-dev | Marquage et actions groupées | Marquage indépendant du pointage et de la confirmation, sélection/inversion, navigation et actions sur un ensemble avec validation adaptée. |
| 0.8.4-dev | Rapprochement avancé | Assistance à la recherche d'écritures correspondant à un solde cible, critères de date/nombre et gestion des références de relevés ; jamais de rapprochement présenté comme certain sans validation. |
| 0.8.5-dev | Soldes prévisionnels complets | Solde aujourd'hui, à une date choisie, futur et minimum prévisionnel ; intégrer les écritures et séries, consolider les comptes choisis, distinguer prévision et réalisé. |
| 0.8.6-dev | Statistiques et graphiques | Revenus/dépenses par période, catégorie, moyen ou compte ; catégories hiérarchiques, dates de valeur, paramètres mémorisés ; exclure les virements internes des flux. |
| 0.8.7-dev | Exports | Export des écritures et statistiques, formats régionaux et CSV ; export chiffré par défaut, CSV en clair seulement après avertissement confirmé à chaque export. |
| 0.8.8-dev | Import bancaire CSV | Choix du compte et des colonnes, date de début incluse et fin facultative, aperçu des écritures existantes et rapprochement anti-doublon avant validation atomique. Traitement local. |
| 0.8.9-dev | Import OFX/QIF | Formats supplémentaires, références bancaires et anti-doublon entre relevés chevauchants ; association aux saisies, séries et virements existants. |
| 0.8.10-dev | Plusieurs bases et propriétés complètes | Créer/ouvrir/cloner/supprimer des bases distinctes, propriétés et notes ; compléter les propriétés des comptes (numéro, notes, sélection pour consolidation) et leurs parcours de gestion. |
| 0.8.11-dev | Maintenance et purge | Contrôles de cohérence et outils de réparation maîtrisés ; purge avec report des soldes ou agrégation, toujours précédée d'une sauvegarde vérifiée. |
| 0.8.12-dev | Personnalisation et préférences | Écran initial, champs visibles, présentation selon les types ; langue/région/formats configurables dans l'application et réglages fonctionnels inclus dans les sauvegardes. |
| 0.8.13-dev | Widgets et raccourcis Android | Accès rapide à la saisie et aux fonctions utiles ; actions sensibles après déverrouillage, aucune information financière exposée par défaut. |
| 0.8.14-dev | Gestion complète du coffre | Modification du code supplémentaire, parcours de changement de protection Android, clés invalidées, récupération depuis archive et réglages de sécurité clairement gérés. |
| 0.8.15-dev | Rapports de plantage locaux | Collecte limitée et expurgée, consultation et partage volontaire depuis l'application ; aucun envoi automatique. Les journaux techniques actuels ne constituent pas encore ce dispositif. |
| 0.9.x-dev | Bêta de parité et préparation de distribution | Audit exhaustif, corrections, migrations, récupération, tests multiappareils, accessibilité, documentation, licences, dépôt public/CI et signature de distribution. Pas de nouvelles grandes familles de fonctions pendant la stabilisation. |
| 1.0.0 | Version stable | Fonctions Palm retenues couvertes ou adaptations explicitement acceptées, critères de stabilité/sécurité/récupération satisfaits et accord de publication. |

## Ajustements demandés après cette proposition

- Pas de livraison utilisateur 0.7.5-dev dédiée au stockage : intégration dans
  0.7.6-dev avec les catégories/moyens avancés. Les contrôles de migration et
  de non-régression restent automatisés ; pas de séance de test technique séparée.
- L'import de relevés bancaires est requis avant 1.0.0 : CSV en 0.8.8-dev puis
  OFX/QIF en 0.8.9-dev. Les lots suivants de 0.8 sont décalés de deux numéros.
  Voir [le parcours et les preuves historiques](import-releves-bancaires.md).
- Présence historique d'un import direct non confirmée dans les sources Palm
  examinées ; ne pas présenter cet ajout comme une parité historiquement prouvée.

## Critères de passage de la série 0.9 à 1.0.0

- **Parité** : reprendre chaque ligne de la matrice Palm et ses workflows,
  préciser les comportements encore seulement identifiés statiquement, tester
  les fonctions et consigner les adaptations ou exclusions acceptées. Un numéro
  0.9 ne vaut pas certification d'exhaustivité.
- **Comptabilité** : cohérence des montants/soldes, arrondis de devises, ventilation,
  paires de virements, séries, projections, rapports et imports. Réimportations,
  relevés chevauchants et rapprochements avec l’existant sans doublons ni
  suppression arbitraire de deux dépenses légitimes. Historique jamais modifié
  implicitement lors d'une édition de série.
- **Récupération** : restaurations réelles ASUS et Samsung A36, notamment après
  perte/invalidation des clés ; archives anciennes, mauvais mot de passe,
  altérations, stockage plein, arrêt de processus et migrations interrompues.
  Vérifier le contenu fonctionnel des sauvegardes, les réglages liés à l'appareil
  à reconfigurer et la rotation des huit archives hebdomadaires.
- **Sécurité** : revue du chiffrement/authentification, verrouillage, captures,
  aperçu, sauvegardes Android exclues, temporaires/journaux/rapports expurgés,
  notifications/widgets et dépendances. Les tests actuels ne remplacent pas cette
  revue complète. Aucun audit externe payant n'est présupposé par ce plan.
- **Ergonomie** : ASUS, Samsung, tablette et Android minimum pris en charge ;
  portrait/paysage, clavier, grand texte, accessibilité, FR/EN et formats régionaux.
  Appliquer le skill visuel Opérations pour toutes les extensions de cette vue.
- **Distribution** : préparer la clé de signature stable et sa sauvegarde,
  une procédure de mise à jour/récupération, l'APK signé, documentation utilisateur,
  licences/attributions et chaîne de build reproductible. Configurer le dépôt
  GitHub public et la CI : aucun remote n'est présent dans cette copie au 11 novembre.
  Publication stable, tags et mise sur une boutique restent sur instruction explicite.

Les tests et la sécurité accompagnent chaque lot : la série 0.9 est une validation
transversale finale, pas le début de ces travaux. Chaque lot comprend FR/EN,
migrations compatibles, sauvegardes adaptées, tests ciblés, nouveautés et essais
utilisateur. Les 115 tests unitaires et 7 tests ASUS de 0.7.4 sont un point de
départ, pas la totalité des preuves nécessaires à 1.0.0.

## Après 1.0 / hors de ce portage initial

Connexion bancaire facultative à étudier après parité (conditions de sécurité,
fournisseurs et gratuité à vérifier), version iOS sans implémentation actuelle,
et diffusion Google Play comme chantier distinct. Aucun budget, objectif, OCR,
pièce jointe ou partage n'est ajouté à la parité sans décision explicite.

## Sources et portée de l'inventaire

Basé sur `cadrage-2026-11-08.md`, `audit-workflows-palm-2026-11-08.md`, les validations
0.1 à 0.7.4 et le code courant (commit de référence `ad98724`). Les noms de lots
restent proposés ; ce document n'implémente aucune fonctionnalité et ne déclenche
ni nouvelle compilation ni installation. Le dernier message de Frédéric valide
le fonctionnement de 0.7.4 et demande l'affichage de ce qui reste jusqu'à 1.0.0.

Actualisation : regroupement de 0.7.5 dans 0.7.6 et import bancaire ajouté avant
1.0.0 à la demande de Frédéric. Application toujours en 0.7.4-dev.

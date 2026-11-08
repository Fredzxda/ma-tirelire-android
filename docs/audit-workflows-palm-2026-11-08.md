# Ma Tirelire — vérification des soldes et de la saisie Palm

8 novembre 2026. Audit statique ciblé de la version Palm 2.1.2 publiée dans
`maxatome/ma-tirelire`, branche master. Aucun émulateur Palm exécuté.
Ce document complète les parcours proposés ; il ne certifie pas encore l'ensemble
des workflows de l'application originale.

## Retour sur la démonstration Android

Observation utilisateur : lancement et navigation fonctionnent ; le solde suit
les opérations ajoutées. La rotation pendant la saisie ferme le formulaire.
Ces constats ne valent pas validation du chiffrement ni de la parité Palm.

## Soldes : fonctionnement confirmé

- `structs.h`, lignes 168–182 : modes VIEW_ALL, VIEW_WORST, VIEW_TODAY,
  VIEW_CHECKED, VIEW_MARKED, VIEW_CHECKNMARKED, VIEW_DATE et VIEW_TODAY_PLUS.
- `TransScrollList.m`, lignes 1611–1706 : une somme de compte calculée selon le
  mode choisi ; VIEW_CHECKED ajoute uniquement les enregistrements pointés,
  VIEW_ALL les ajoute tous. Les modes datés utilisent la date de valeur si présente.
- `Transaction.m`, vers 397 : le solde initial du compte est représenté par un
  enregistrement toujours pointé. Il fait donc partie du solde pointé.
- `AccountsScrollList.m`, lignes 296–389 : même sélection de mode appliquée aux
  soldes des comptes, avec inclusion du solde initial dans le mode pointé.
- `SumListForm.m`, lignes 180–278 : liste de sélection du type de solde, mise à
  jour de `ul_sum_type`, recalcul et rafraîchissement après sélection.
- `obj.rcp`, lignes 422–430 et 1198–1205 : sélecteur de mode et un champ
  `SumListSum` dans les écrans opérations et comptes.

Conclusion : le solde pointé existe bien dans l'original ; la démonstration ne
le calcule et ne l'affiche pas encore. Les sources examinées montrent un solde
principal sélectionnable, pas la preuve d'un affichage simultané total + pointé.
Ne pas transformer ce dernier souvenir en fait historique sans autre vérification.
Afficher les deux simultanément sur Android serait une adaptation utile proposée.

Définitions à retenir pour le cas simple, hors conversions et filtrage temporel :

- Solde total du compte = solde initial + toutes les écritures signées.
- Solde pointé = solde initial + écritures pointées signées.
- Pointer ne modifie pas le solde total ; cela modifie le solde pointé du montant
  signé. Dépointer effectue l'inverse.
- Le total des comptes affichés est une consolidation ; c'est un autre périmètre
  que le solde total d'un compte. Les libellés Android doivent l'expliciter.

Scénario de validation proposé : initial 1 000,00 EUR ; revenu pointé +100,00 EUR,
dépense pointée -12,34 EUR, dépense non pointée -20,00 EUR. Total 1 067,66 EUR,
pointé 1 087,66 EUR. Pointer -20,00 ramène le pointé à 1 067,66 sans changer le
total. Dépointer rétablit 1 087,66. Vérifier ensuite les variantes dates/devises.

Sources :
- https://github.com/maxatome/ma-tirelire/blob/master/structs.h#L168
- https://github.com/maxatome/ma-tirelire/blob/master/TransScrollList.m#L1611
- https://github.com/maxatome/ma-tirelire/blob/master/Transaction.m#L397
- https://github.com/maxatome/ma-tirelire/blob/master/AccountsScrollList.m#L296
- https://github.com/maxatome/ma-tirelire/blob/master/SumListForm.m#L180
- https://github.com/maxatome/ma-tirelire/blob/master/obj.rcp#L422

## Catégorie, type, moyen et description : ne pas les confondre

| Terme | Sens dans les sources Palm | Démonstration Android actuelle |
|---|---|---|
| Débit/crédit | Signe de l'écriture | Choix Dépense/Revenu |
| Type d'opération (`Type`) | Catégorie hiérarchique, filtrable selon débit/crédit et compte | Champ texte Catégorie, sans référentiel |
| Mode (`Mode`) | Moyen de paiement | Champ texte Moyen de paiement, sans référentiel |
| Description (`Desc`) | Texte d'écriture et descriptions/macros réutilisables | Description libre uniquement |

Le type Palm et la catégorie Android sont donc une correspondance de vocabulaire,
pas deux champs à ajouter en doublon. Le nom « catégories » des API Palm désigne
parfois les comptes : distinguer ce détail technique des catégories budgétaires.

### Sélection et création

- `obj.rcp`, lignes 543–553 et 591–627 : description en champ libre avec liste de
  descriptions/macros ; type et mode sous forme de sélecteurs/listes.
- `TransForm.m`, lignes 1427–1460 : liste des types construite selon le signe et
  le compte. Une simple chaîne de catégorie sur chaque opération ne restitue pas
  ce fonctionnement.
- `TransForm.m`, lignes 4159–4176 et 4202–4212 : les listes de mode/type donnent
  accès à l'édition via `ModesListForm` / `TypesListForm`.
- `TypesListForm.m`, lignes 232–262 : commandes Nouveau et Nouveau sous-type
  ouvrant le formulaire d'édition de catégorie.
- `DescModesListForm.m`, lignes 202–207 : commande Nouveau ouvrant l'édition
  d'un moyen de paiement ou d'une description selon la liste gérée.
- `TypesListForm.m`, lignes 335–372 et `TransForm.m`, lignes 4749–4780 :
  rafraîchissement après modification ; l'entrée créée est retrouvée dans le
  gestionnaire et les listes de saisie sont rechargées. La sélection automatique
  de cette nouvelle entrée dans le champ final de l'opération n'est pas établie.

Confirmé : sélection dans un référentiel et création via ses écrans de gestion.
La saisie libre d'un nom inconnu dans le champ type/mode, créant automatiquement
une entrée, n'est pas établie par cet examen. Ne pas la promettre comme comportement
Palm. La description libre ne doit pas être confondue avec cette création.

Sources :
- https://github.com/maxatome/ma-tirelire/blob/master/obj.rcp#L543
- https://github.com/maxatome/ma-tirelire/blob/master/TransForm.m#L1427
- https://github.com/maxatome/ma-tirelire/blob/master/TransForm.m#L4159
- https://github.com/maxatome/ma-tirelire/blob/master/TypesListForm.m#L232
- https://github.com/maxatome/ma-tirelire/blob/master/DescModesListForm.m#L202
- https://github.com/maxatome/ma-tirelire/blob/master/TypesListForm.m#L335
- https://github.com/maxatome/ma-tirelire/blob/master/TransForm.m#L4749
- https://github.com/maxatome/ma-tirelire/blob/master/obj-fr.rcp#L186

## Corrections proposées pour la prochaine itération d'interface

1. Présenter clairement solde total et solde pointé du compte ; qualifier l'affichage
   simultané d'adaptation Android, en conservant les règles historiques de calcul.
2. Remplacer les champs libres catégorie/moyen par des sélecteurs de référentiels.
3. Prévoir une action explicite Ajouter depuis ces sélecteurs et un retour dans
   la saisie sans perte du brouillon ; cette réduction d'étapes est une adaptation.
4. Conserver le brouillon lors d'une rotation, avec état mémoire uniquement pour
   la démo ; aucune persistance sensible en clair.
5. Tester création → sélection → enregistrement → nouvelle saisie : l'entrée doit
   être réutilisable, sans doublon involontaire ; hiérarchie et filtrages doivent
   être spécifiés, même s'ils sont livrés dans un lot ultérieur.

Avant de déclarer la saisie conforme : vérifier retour exact depuis les écrans de
gestion, sélection de l'entrée créée, annulation, valeurs par défaut, macros et
effets des changements de catégorie/moyen. Audit de rapprochement, édition,
suppression et autres workflows encore à compléter.

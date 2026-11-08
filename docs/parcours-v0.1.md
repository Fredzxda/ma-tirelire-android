# Ma Tirelire — parcours et critères initiaux

8 novembre 2026. Proposition de spécification ; démonstration 0.1.0-dev partielle
désormais implémentée, sans équivalence complète avec les parcours ci-dessous.
Référence : `cadrage-2026-11-08.md`. Périmètre : préparation des lots 0.1 à 0.3.

Vérification ciblée après premiers essais :
[soldes, catégories et moyens de paiement Palm](audit-workflows-palm-2026-11-08.md).
Le lancement et la mise à jour du solde ont été observés par l'utilisateur ; le
brouillon de saisie est encore perdu à la rotation dans la démonstration.

## Origine et statut des règles

Ce document s'appuie sur l'inspection statique des sources de Ma Tirelire Palm
2.1.2 recensées dans le cadrage. L'application Palm n'a pas été exécutée : les
parcours ci-dessous ne constituent pas une restitution vérifiée de ses écrans,
de leur enchaînement ou de tous leurs comportements.

Chaque section distingue les origines suivantes :

- **Palm confirmé dans les sources** : présence d'une fonction ou d'un champ,
  avec référence ; cela ne confirme pas tous les détails de son fonctionnement.
- **Exigence utilisateur validée** : décision exprimée pour le portage, sans
  présumer que la version Palm fonctionnait ainsi.
- **Adaptation Android proposée** : ergonomie ou comportement à discuter avant
  de figer la spécification ; une proposition n'est pas une décision validée.
- **À vérifier** : règle métier ou comportement historique à établir depuis les
  sources, puis à traduire en scénario de validation avant implémentation.

Les exemples chiffrés sont des jeux de validation proposés pour Android, pas des
résultats observés sur Palm. Une fonction ne sera déclarée conforme à l'original
qu'après vérification de ses règles et validation des adaptations éventuelles.

## Navigation proposée

**Adaptation Android proposée** : organisation des destinations, bouton de saisie,
navigation tablette et conservation de l'état. **Exigence utilisateur validée** :
portrait/paysage et absence d'informations financières dans l'aperçu verrouillé.

Téléphone : destinations Comptes, Opérations et Réglages ; bouton Saisir visible
depuis les comptes et opérations. Statistiques et Échéances ajoutées avec leurs lots.
Tablette : navigation latérale et liste/détail quand la largeur le permet.
Portrait/paysage : conserver compte sélectionné, filtre et brouillon, sans stockage
de contenu sensible dans les mécanismes système non chiffrés de sauvegarde d'état.
Écran verrouillé : aucun nom de compte, montant ou dernière opération.

## Première utilisation

**Palm confirmé dans les sources** : comptes avec devise et solde initial
([Transaction.h](https://github.com/maxatome/ma-tirelire/blob/master/Transaction.h)).
**Exigences utilisateur validées** : langues, stockage local chiffré,
authentification et sauvegardes. **Adaptation Android proposée** : assistant,
ordre des étapes et possibilité de différer la configuration de sauvegarde.
**À vérifier** : sémantique historique de la date de départ du compte.

1. Choisir langue et région, proposées selon Android ; devise proposée selon région.
2. Expliquer brièvement le stockage local et le déverrouillage Android obligatoire.
3. Vérifier qu'un verrouillage système est configuré ; sinon guider vers les réglages.
4. Configurer éventuellement empreinte et code supplémentaire.
5. Créer le coffre et le premier compte : nom, devise et solde initial, date de départ.
6. Proposer configuration de sauvegarde : dossier local, mot de passe confirmé,
   fréquence hebdomadaire et conservation de huit archives automatiques.

Proposition : la sauvegarde ne bloque pas la découverte avec données fictives ;
le statut « sauvegarde non configurée » reste visible tant qu'elle manque.
Le mot de passe de sauvegarde n'est ni demandé dans une issue GitHub ni journalisé.

## Saisir une opération

**Précision historique vérifiée** : le « type d'opération » Palm correspond à la
catégorie, distincte du signe débit/crédit et du moyen de paiement. Types et moyens
se sélectionnent dans des listes, avec accès à leurs écrans de gestion/création.
La création automatique par simple saisie libre d'un nom n'est pas établie.
Les champs texte Catégorie/Moyen de la démo sont des simplifications à remplacer
par des sélecteurs ; proposition Android : action Ajouter et retour sans perte
du brouillon. Voir l'audit lié ci-dessus pour les sources et limites.

**Palm confirmé dans les sources** : débit/crédit, montant, date, description,
catégorie, moyen de paiement et date de valeur
([Transaction.h](https://github.com/maxatome/ma-tirelire/blob/master/Transaction.h),
[ressources des formulaires](https://github.com/maxatome/ma-tirelire/blob/master/obj-en.rcp)).
**Adaptation Android proposée** : ordre des champs, présélections, champs repliés,
choix Dépense/Revenu, retour à la liste et gestion des brouillons.
**À vérifier** : règles Palm de signe, valeurs initiales, validation, arrondis,
édition et calcul des différents soldes, notamment avec les dates de valeur.

Depuis un compte : bouton Saisir → Dépense/Revenu → montant, date, description,
catégorie et moyen de paiement → Enregistrer. Compte courant et date du jour
présélectionnés ; champs avancés repliés. Date de valeur et devise suivent leurs
règles quand les fonctions correspondantes sont disponibles.

Le signe résulte du choix Dépense/Revenu : pas de double négatif ambigu.
Saisie française avec virgule et anglaise avec point selon les réglages.
Validation interdit montant invalide, dépassement de capacité et double enregistrement.
Après succès : retour à la liste, nouvelle ligne visible et solde mis à jour.
Un échec ne perd pas le brouillon de la session et n'insère pas de ligne partielle.
Si l'application est réellement mise en arrière-plan : verrouiller ; conserver un
brouillon uniquement selon une stratégie chiffrée à spécifier, jamais en clair.

Exemple fictif de validation : compte à 1 000,00 EUR, dépense de 12,34 EUR puis
revenu de 100,00 EUR → solde comptable 1 087,66 EUR. Modifier la dépense à 20,00 EUR
→ 1 080,00 EUR. Supprimer le revenu après confirmation → 980,00 EUR.
Les écritures futures et dates de valeur font l'objet de soldes séparés et ne
doivent pas modifier silencieusement le sens du solde affiché.

## Confirmer une écriture bancaire : pointage

**Palm confirmé dans les sources** : état de pointage distinct du marquage,
rapprochement avec solde cible et numéro de relevé facultatif, commandes de
confirmation/annulation et recherche automatique de combinaisons
([Transaction.h](https://github.com/maxatome/ma-tirelire/blob/master/Transaction.h),
[ClearingListForm.m](https://github.com/maxatome/ma-tirelire/blob/master/ClearingListForm.m),
[ClearingScrollList.m](https://github.com/maxatome/ma-tirelire/blob/master/ClearingScrollList.m)).
**Adaptation Android proposée** : vocabulaire et enchaînement présentés ci-dessous.
Ils ne prouvent pas que Palm exigeait un solde cible pour pointer une seule écriture.
**À vérifier avant validation de ce parcours** : accès au pointage individuel,
distinction pointage/rapprochement, calcul de l'écart, traitement des opérations
déjà pointées, persistance des choix, effet exact de Confirmer/Annuler et possibilité
de terminer avec un écart non nul. Les comportements ci-dessous restent proposés
jusqu'à cette vérification ; tout écart retenu sera documenté comme adaptation.

Proposition de vocabulaire : « Pointer » signifie reconnaître une écriture sur le
relevé ; « Rapprocher » signifie vérifier un ensemble contre le solde bancaire.
Le marquage général historique reste une autre propriété, sans confusion visuelle.

Depuis le compte : Pointer → saisir solde bancaire et éventuellement numéro de
relevé → sélectionner les opérations présentes sur le relevé → voir l'écart restant
→ confirmer le rapprochement. L'écart nul est clairement indiqué ; un écart non nul
ne doit pas être présenté comme un rapprochement réussi.
Les choix restent provisoires jusqu'à confirmation ; Annuler restitue l'état initial.
Le pointage seul ne change pas le montant ni le solde comptable.
Il modifie en revanche le **solde pointé**, qui inclut le solde initial et les
écritures pointées signées : cette règle de calcul est confirmée dans les sources.
La démo n'affiche encore que le solde total. L'original permet de choisir le mode
de solde ; afficher total et pointé simultanément sur Android reste une adaptation
proposée, pas un affichage Palm confirmé.
La détection automatique de combinaisons attend le lot 0.5.

Exemple fictif : départ 1 000,00 EUR ; salaire +100,00 et achat -12,34 présents sur
le relevé ; autre achat -20,00 absent. Solde comptable : 1 067,66 EUR ; solde du
relevé : 1 087,66 EUR ; pointer seulement les deux premières lignes donne écart nul.
Les règles exactes de solde d'ouverture et d'inclusion des opérations déjà pointées
seront spécifiées contre les sources Palm avant implémentation.

## Sauvegarder

**Exigences utilisateur validées** : archive locale chiffrée, sauvegardes manuelles,
hebdomadaire due exécutée après déverrouillage et huit archives hebdomadaires.
**Adaptations Android proposées** : écrans, gestion des erreurs et articulation avec
les dialogues système. Le calendrier indépendant des sauvegardes manuelles reste
une proposition. Aucun de ces parcours n'est présenté comme un comportement Palm.

Manuel : Réglages → Sauvegardes → Sauvegarder maintenant → résultat daté et taille.
Automatique : après authentification complète, si sept jours écoulés depuis la
dernière sauvegarde hebdomadaire réussie, créer une archive cohérente puis faire
la rotation. Une sauvegarde manuelle ne supprime jamais une archive manuelle.
Choix proposé : les manuelles ne décalent pas le calendrier des hebdomadaires.

Le sélecteur système et les changements d'activité nécessaires aux parcours
fichiers ne doivent pas provoquer une boucle d'authentification. En revanche,
un vrai passage en arrière-plan verrouille ; le retour reprend via authentification.
Une rotation d'écran seule ne doit pas invalider une session.

Tester : stockage plein, dossier disparu, permission retirée, interruption pendant
écriture et reprise. L'application distingue échec et succès ; ancienne archive
conservée tant que la nouvelle n'est pas validée. Aucun fichier financier clair
intermédiaire. Date de dernière réussite affichée sans faux succès.

## Restaurer

**Exigence utilisateur validée** : récupération sur un autre téléphone au moyen
d'une sauvegarde et de son mot de passe. **Adaptations proposées** : étapes,
sauvegarde préalable au remplacement et stratégie de restauration. Le test croisé
est un critère de validation proposé ; aucune restauration n'a encore été exécutée.

Disponible au premier lancement et dans Réglages → Sauvegardes.
Choisir archive → mot de passe → vérifier intégrité et version → présenter portée
du remplacement → confirmer explicitement → restaurer et réauthentifier si nécessaire.
Avant remplacement d'une base existante : produire une sauvegarde de sécurité,
sans l'écraser par la rotation ; annuler si cette protection ne peut pas être créée.

Test bloquant du lot 0.2 : sauvegarde faite sur ASUS, restaurée sur Samsung avec
le seul fichier et son mot de passe, sans clé du premier téléphone. Vérifier données
et réglages. Mauvais mot de passe, fichier tronqué/altéré ou format incompatible :
refus sans modification de l'existant. Ne jamais effacer le coffre avant validation.
Les accès biométriques/code supplémentaire du nouvel appareil sont réenrôlés ;
on ne restaure pas les autorisations et secrets matériels de l'ancien téléphone.

## Export CSV en clair

**Palm confirmé dans les sources** : export de listes/statistiques en texte CSV
vers Memo Pad ([ExportForm.m](https://github.com/maxatome/ma-tirelire/blob/master/ExportForm.m)).
**Exigence utilisateur validée** : export en clair seulement après acceptation de
l'avertissement. **Adaptation Android proposée** : export vers un fichier et texte
de l'avertissement. **À vérifier** : colonnes, échappement et formats historiques.

Choisir périmètre et CSV en clair → avertissement avant toute création de fichier :

> Ce fichier contiendra vos données financières sans chiffrement. Toute personne
> ou application qui y accède pourra les lire. Vérifiez où vous l'enregistrez et
> à qui vous le transmettez.

Actions : Annuler / Exporter sans chiffrement. Aucun « Ne plus afficher ».
Annuler ne produit aucun fichier ; confirmer permet le choix de destination.
L'application ne conserve pas de copie claire supplémentaire. Le fichier demandé
reste sous le contrôle de l'utilisateur après export ; il n'est plus protégé par
le verrouillage de l'application. Même règle pour les exports de statistiques.

## Critères avant premier usage personnel avec données réelles

**Critères de validation proposés pour le portage** : cette liste n'est ni une
description des garanties Palm ni un compte rendu de tests déjà réalisés.

- Règles Palm des parcours concernés vérifiées, références consignées et écarts
  d'adaptation explicitement validés, particulièrement pour pointage/rapprochement.
- Calculs monétaires, édition, suppression et pointage validés sur exemples fictifs.
- Authentification, relock et aperçu vérifiés sur les deux téléphones.
- Sauvegarde et restauration croisée réussies, archive altérée rejetée.
- Données sensibles absentes des fichiers clairs, journaux et rapports techniques.
- Comportement des sauvegardes cloud/transferts Android vérifié autant que possible,
  limites de vérification consignées séparément de la configuration déclarative.
- FR/EN et rotation testés, clavier utilisable au doigt, aucun bouton essentiel masqué.
- Clé de signature conservée et procédure de récupération documentée.
- Fonctions manquantes de la version clairement indiquées avant usage.

À discuter lors de la revue des maquettes : vocabulaire pointage/rapprochement,
ordre des champs, contenu de la liste des comptes et affichage des soldes.

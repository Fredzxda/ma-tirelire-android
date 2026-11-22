# Validation 0.7.15-dev — 22 novembre 2026

Version compilée et installée : 0.7.15-dev, versionCode 45.

## Périmètre

- Opérations : catégories ventilées sans préfixe et virement interne avec une
  seule métadonnée, dans la typographie habituelle.
- Fiche : montant + devise, description + moyen de paiement, puis catégorie et
  ventilation. Les taux de change et cibles de virements restent disponibles.
- Informations de périodicité et de limites dans un dialogue défilant ; les
  champs et erreurs restent accessibles dans la fiche.
- Libellés applicatifs FR/EN actualisés : application « Ma Tirelire », plus de
  mentions « fictif », « démonstration » ou « prototype » à l’écran.
- Catalogue d’icônes et maquette consultables avant leur intégration aux données.

Pas de modification du format du coffre, du package Android ou des données
existantes. Les anciennes descriptions enregistrées ne sont pas renommées.
Les exemples et tests restent séparés du coffre personnel.

## Vérifications

- Compilation application et APK de tests réussie ; lint réussi.
- 186 tests unitaires réussis, aucune erreur ni test ignoré.
- Installation vérifiée : 0.7.15-dev, versionCode 45 ; application relancée.
- 23 tests instrumentés SQLCipher/Keystore et interface réussis sur ASUS.
  Les nouvelles vérifications portent sur la disposition portrait/paysage,
  l’empilement avec texte agrandi, le dialogue Informations, la réutilisation
  d’un moyen de paiement et les métadonnées des cartes. Activité isolée et
  données de test uniquement ; le coffre personnel n’est pas ouvert par les tests.
- Revue indépendante : deux points corrigés avant livraison (test grands
  caractères et sélection d’un paiement existant).
- Catalogue : 64 identifiants uniques et SVG valides, JavaScript valide,
  64 boutons rendus dans Chrome ; capture desktop inspectée. Recherche, famille,
  ventilation +N et masquage vérifiés dans Chrome en dimensions mobiles.
- Placement des icônes validé par Frédéric le 22 novembre : avant le statut,
  trois pictogrammes distincts maximum puis +N. Association persistée et
  affichage Android au prochain lot ; la maquette n’est pas une capture de l’APK.

Les tests de composants ne prouvent pas toute la fiche, le clavier réel ou
toutes les rotations ; ces variantes restent dans les essais manuels ci-dessous.
Avertissements existants de dépréciation des API Compose de tests. CI GitHub non
observée lors de cette validation locale.

## À vérifier sur le téléphone

1. Saisir puis modifier une opération : largeur du montant et de la description,
   sélection des devises et du moyen, accès à la dernière action en paysage.
2. Vérifier taux de change et compte cible d’un virement interne, puis sa carte :
   une seule ligne « Virement interne lié ».
3. Ventiler une opération : catégorie suivie immédiatement des lignes, catégories
   réunies sans préfixe dans la liste.
4. Ouvrir Informations dans Périodicité : explications présentes dans le dialogue,
   absentes de la fiche, paramètres et calendrier toujours utilisables.
5. Vérifier avec le clavier ouvert et le texte Android agrandi. Les tests de
   composants ne suffisent pas à prouver toute la fiche et toutes les rotations.

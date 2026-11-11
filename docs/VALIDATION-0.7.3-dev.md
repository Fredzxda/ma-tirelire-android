# 0.7.3-dev — modele visuel Operations valide

Proposition validee explicitement par Frederic le 11 novembre 2026 :
« C est parfait. Tu peux implementer. » Reference docs/design/operations-proposition.md.
Skill projet et copie installee actualises ; statut de validation conserve.

## Presentation et interactions

Compte/soldes fixes et + dans le bandeau. En portrait, grand BrandHeader retire
de cette vue ; ailleurs il est conserve. En paysage, rail gauche bleu fonce.
Une seule barre Tri/Compte/Filtre/Recherche/Fonctions ; un seul sous-menu temporaire
en dessous, selections en ligne a defilement horizontal. Retoucher le menu ou
cliquer hors ligne la referme, sans effacer ses criteres. Observation du relachement
apres les enfants, sans consommation des evenements ni suppression sur appui :
pas de premier clic perdu, pas de deplacement de la cible pendant le geste.
Les callbacks accessibles de cartes et pointage ferment aussi explicitement la ligne.

Recherche integree : compte ET periode ET texte. Ancienne fenetre supprimee.
Historique elargit la recherche ; Effacer retire le texte seul. Indice de recherche
active et periode dans le menu, aucun paragraphe explicatif permanent.
Informations regroupe les explications et dispose d un texte defilant.
Menu et criteres en memoire du ViewModel pour la rotation ; effaces/reinitialises
au verrouillage. Aucun champ de recherche ou sous-menu dans SavedState en clair.

Carte : statut de serie haut gauche et date haut droite ; description/montant
alignes en haut ; categorie/moyen sous description, pointage cible 48 dp sous montant.
Virement interne sans categorie affichee ; date illisible signalee. Les calculs,
pointages, series, sauvegardes et le schema du coffre ne changent pas.

Les limites de periodicite utilisent le champ Material state-based avec
TextFieldLabelPosition.Attached(alwaysMinimize = true), etat conserve seulement
en memoire, brouillon chiffre restant autoritaire. Label toujours dans l encoche,
meme typographie et hauteur a vide, au focus et renseigne. A petite largeur ou
texte agrandi, les champs sont empiles plutot que rognes. Date par calendrier
natif protege ; controle accessible comme bouton. Fin/date et nombre restent exclusifs.

## Verification

Tests unitaires metier existants, ajout du reset du menu au controle de session.
Quatre tests Compose isoles dans une activite vierge : combinaison recherche/periode,
fermeture conservant criteres ; premier clic carte/pointage sans double action ;
clic sur zone vide sans perte du texte ; hauteur des champs avant/apres saisie
et choix programmatique de fin de serie. Test APK et activite de test limites au debug.
Play selectionne uniquement VaultStorageDeviceTest et OperationsUiDeviceTest,
controle leurs resultats puis relance la vraie application sans ouvrir son coffre.

Essais Frederic : portrait/paysage, les quatre menus, clic exterieur sur carte,
pointage et fond, clavier logiciel, rotation, texte agrandi et grands montants ;
filtre et recherche combines puis effaces, ajout via +, rapprochement/echeances,
et periodicite vide/remplie/dates/calendrier. Observation visuelle reelle distinguee
de l inspection des noeuds Compose. Aucune capture du coffre personnel demandee.

Retour arriere code : 7f73139 (0.7.2-dev), proposition seule 863d0a4.
Conserver une archive chiffree ; aucun changement du schema 11 ni de donnees.
Ne pas desinstaller l application pour revenir en arriere.

Validation finale : compilation des deux APK, 115 tests unitaires et lint reussis.
ASUS reconnecte : installation 0.7.3-dev verifiee et sept tests reussis (trois
SQLCipher/Keystore, quatre Compose). Application relancee avec statut de succes.
Premier passage : la geometrie des champs a revele l indication de date vide
trop longue. Indication raccourcie, placeholder limite a une ligne, mesures des
cadres externes harmonisees : les tests de hauteur passent a vide, pendant la
saisie et avec une date renseignee. Les labels restent toujours minimises.
Les tests sont inclus dans Play pour les prochaines compilations. Rendu visuel,
calendrier natif et ergonomie portrait/paysage avec donnees utilisateur a verifier
par Frederic ; aucune lecture ni capture de son coffre par les tests.

# 0.6.0-dev — recherche textuelle

Premier lot recherche de la feuille de route : compte courant sélectionné,
description/catégorie/moyen de paiement, mots combinés par ET, accents et casse ignorés.
Ordre et identité des écritures conservés. Aucun changement de schéma ni de données.
Le nombre et la somme des résultats incluent les prévisions et virements affichés ;
ce ne sont pas des statistiques de revenus/dépenses. Soldes globaux inchangés.
La recherche survit à la rotation en mémoire du ViewModel, jamais en SavedState
ni dans une préférence en clair ; effacée au verrouillage.

À tester : recherche accentuée, mots sur plusieurs champs, aucun résultat,
effacement, édition depuis les résultats, compte différent, rotation et verrouillage.
Les filtres avancés dates/montants/statuts et opérateurs OU restent à venir.

Retour arrière : conserver une sauvegarde chiffrée ; aucun changement de format
introduit par cette version. Ne pas désinstaller pour revenir à une version précédente.

Validation locale : build et lint reussis, 97 tests unitaires reussis.
ASUS : installation 0.6.0-dev verifiee, 3 tests SQLCipher/Keystore isoles
reussis, lancement confirme. Validation visuelle et ergonomique par Frederic a venir.

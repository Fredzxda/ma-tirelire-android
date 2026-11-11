# 0.7.0.1-dev — tri des anciennes dates

Signalement : tri recent, fin de liste 2 nov. 2026, 2 nov. 2026, 10 nov. 2026.
Cause reproduite avec deux dates ISO et une date historique localisee. Avant
11ade20 (0.3.5), EntryActions enregistrait LocalDate.now formate en MEDIUM selon
la locale ; TransactionOrder ne reconnaissait que ISO et placait le texte en fin.
Le format reel du coffre utilisateur n a pas ete lu.

Correction de presentation uniquement : parse strict des dates MEDIUM avec mois
en lettres des locales FR/US/UK, independamment de la locale courante, sans
migration ni remplacement des dates, sans modification de la saisie/date picker.
Dates numeriques ambigues, invalides et exemples sans annee restent non interpretes.
Tests : cas signale, ISO/localise dans les deux sens, FR/US/UK, fevrier bissextile,
rejet des dates invalides et absence de mutation. Aucune modification de schema.
Retour arriere code : 539a4c3 ; conserver sauvegarde chiffree, ne pas desinstaller.
Les raccourcis 0.7.1 et navigation mensuelle 0.7.2 restent au planning.

Validation : compilation, 102 tests unitaires et lint reussis. ASUS : version
0.7.0.1-dev installee et verifiee, trois tests de stockage isoles reussis,
application relancee. Confirmation du cas utilisateur sur son coffre attendue.

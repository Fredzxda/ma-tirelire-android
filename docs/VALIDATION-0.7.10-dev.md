# Validation 0.7.10-dev — 13 novembre 2026

- Lot : devises et liens de virement ; pas de ventilation dans cette livraison.
- Compilations Debug et APK de tests, lint : réussis.
- 148 tests unitaires : réussis (précisions EUR/JPY/KWD, arrondis au pair,
  taux stricts, transferts atomiques et édition des deux côtés, quote réciproque
  extrême, original figé, séries, consolidation, dissociation, archives et métadonnées).
- 11 tests ASUS : réussis, dont saisie d’une dépense USD comptabilisée en EUR,
  conservation du montant d’origine, défilement en paysage et contrôles isolés
  SQLCipher/Keystore. Aucun accès au coffre personnel pour les tests.
- Harnais : installation 0.7.10-dev et relancement confirmés.
- Skill : quick_validate réussi ; source dans le dépôt et copie personnelle
  identiques. AGENTS.md route les évolutions de formulaires vers ce skill.

Limites : les succès automatisés ne remplacent pas les essais utilisateur de
la nouvelle version, la restauration réelle multidevise, le Samsung A36, les
tablettes et les caractères Android agrandis. Source et scénarios :
[Devises](devises-0.7.10.md). Aucun tag ni publication stable.

Dernier etat de livraison : le candidat avec les devises a ete installe et a passe les 11 tests ASUS. Un dernier ajustement compare les taux numeriquement pour eviter un dialogue de serie lors du seul changement de format 0,9/0,90 ; les 148 tests unitaires et lint passent avec cet ajustement. Sa reinstallation attend le retour de la connexion ADB. Ne pas assimiler les 11 tests du candidat precedent a une execution sur ce dernier APK.

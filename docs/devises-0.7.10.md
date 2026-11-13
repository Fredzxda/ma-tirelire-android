# Devises — 0.7.10-dev

## Ce qui change

- Une devise par compte, choisie à la création. Les comptes et archives existants
  restent en EUR. Une devise existante ne peut changer que sur un compte vide,
  sans série ni brouillon associé et avec un solde initial nul : aucun historique
  n'est réinterprété dans une autre unité.
- Devises ISO proposées par Android avec zéro à trois décimales. Saisie stricte
  à cette précision ; aucune troncature silencieuse des montants saisis.
- Dans Réglages, un taux local représente **1 unité de la devise = x EUR**.
  EUR vaut toujours 1. Aucun téléchargement, compte distant ou abonnement.
- En saisie, choisir éventuellement une devise d'origine différente du compte.
  Le taux indiqué représente **1 unité d'origine = x unités du compte**. Le
  montant d'origine, la devise, le taux appliqué et le montant comptable sont
  conservés ensemble. Le taux peut être saisi pour cette seule écriture.
- Pour un virement interne, saisir le montant du compte affiché ; le taux
  représente **1 unité de ce compte = x unités du compte cible**. L'aperçu
  affiche le montant opposé. Les deux écritures restent enregistrées atomiquement,
  avec leur pointage indépendant. On peut éditer depuis l'un ou l'autre compte.
- Conversion à la précision de la devise cible, arrondi au plus proche, au pair
  en cas d'égalité (HALF_EVEN). Un résultat arrondi à zéro ou hors limites est
  refusé. Les montants restent en BigDecimal, jamais en flottants binaires.
- Les séries conservent les montants/taux de leur modèle : changer les taux
  locaux ne recalcule pas les opérations ni les échéances déjà enregistrées.
  L'édition avec portée « série » suit les règles existantes de préservation
  de l'historique ; une paire de virement reste indivisible.
- La consolidation est évaluée en EUR avec les taux locaux actuels, arrondi
  par compte avant addition. Si un taux manque, le total consolidé est remplacé
  par un message ; les soldes natifs des comptes restent visibles. Une variation
  de consolidation n'est pas une réécriture des opérations.
- « Dissocier » dans une fiche de virement demande confirmation. Les deux
  écritures et leurs états sont conservés, deviennent indépendantes et entrent
  dans les flux de revenus/dépenses. Les futures échéances restent des virements.
  Les modifications non enregistrées de la fiche sont abandonnées explicitement.

## Sauvegardes et retour arrière

Les champs multidevises et les taux sont dans le coffre chiffré et les archives
chiffrées. Le schéma 13 conserve la lecture des schémas 1 à 12. Aucun changement
du format cryptographique des archives ni des clés Android.

Faire une sauvegarde avant d'utiliser les devises. Une application antérieure ne
sait pas ouvrir un état au schéma 13 ; pour un retour à cette application,
utiliser une sauvegarde créée avant l'introduction de ces champs. Ne jamais
effacer ou remplacer le coffre pour tenter un retour arrière automatique.

## Vérifications utilisateur

1. Créer un compte USD, avec solde initial zéro. Dans Réglages, saisir 0,90 EUR
   pour 1 USD (0.90 si locale anglaise).
2. Depuis un compte EUR, saisir une dépense d'origine 10 USD : montant du compte
   9 EUR. Modifier le taux local puis rouvrir : les montants enregistrés restent.
3. Virer 10 USD du compte USD vers EUR : débit 10 USD, crédit 9 EUR. Éditer depuis
   le crédit, vérifier le débit correspondant et le pointage indépendant.
4. Essayer une série de virements, puis modifier un montant avec portée future :
   les autres occurrences enregistrées ne changent pas.
5. Sauvegarder puis restaurer et vérifier devises, taux, montants et liens.
6. Vérifier portrait, paysage, clavier et sélecteur de devise recherchable.

Le skill `.agents/skills/ma-tirelire-adaptive-forms/SKILL.md` complète le contrat
Opérations pour les formulaires et sélecteurs. Il est aussi installé dans les
skills personnels Codex, avec états sensibles exclusivement en mémoire/coffre.

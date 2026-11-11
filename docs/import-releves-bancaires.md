# Import de relevés bancaires — préparation avant 1.0.0

Demande de Frédéric du 11 novembre 2026, après la feuille de route de 0.7.4-dev.
Fonction désormais requise avant la 1.0.0, même si son équivalence historique
Palm n'a pas pu être confirmée. Document de planification, aucune implémentation.

## Vérification des sources Palm

Arbre Git examiné : `8b462ab719827950b468a9b19e52106817e73ba8`, 236 entrées,
réponse récursive non tronquée. Pas de fichier d'import CSV/QIF/OFX identifié
dans cet arbre. Lecture du README, des ressources obj-en.rcp, d'ExportForm.m,
de DBasePropForm.m, de MaTirelire.m et du gestionnaire de lancement main.m.

- ExportForm.m, lignes 332 et suivantes : ouverture de la base Memo Pad et
  création des mémos d'export. Les ressources portent Export to Memo Pad.
- DBasePropForm.m, lignes 256-258, 443-444 et 541-542 : réglage d'utilisation
  d'un conduit. Cela prouve le réglage, pas ses formats ni un import bancaire.
- main.m, lignes 141-147 : après synchronisation, vérification/tri des bases.
  Le cas ExgReceiveData, lignes 173-175, n'implémente pas d'import dans ce fichier.
- Aucune preuve directe retrouvée d'un assistant d'import bancaire CSV/QIF/OFX.
  L'absence de preuve dans ce dépôt ne permet pas d'exclure un outil PC séparé
  ou une autre version. Le site historique ma-tirelire.net n'était pas accessible
  par l'outil de consultation pendant cette vérification ; les recherches web
  n'ont pas fourni de documentation primaire confirmant les formats d'import.

Sources primaires :
- https://github.com/maxatome/ma-tirelire/blob/8b462ab719827950b468a9b19e52106817e73ba8/ExportForm.m#L332
- https://github.com/maxatome/ma-tirelire/blob/8b462ab719827950b468a9b19e52106817e73ba8/DBasePropForm.m#L443
- https://github.com/maxatome/ma-tirelire/blob/8b462ab719827950b468a9b19e52106817e73ba8/main.m#L141
- https://github.com/maxatome/ma-tirelire/blob/8b462ab719827950b468a9b19e52106817e73ba8/obj-en.rcp#L987

## Lots prévus

- **0.8.8-dev** : premier import CSV, paramétrable selon les colonnes bancaires,
  avec aperçu, période et prévention des doublons.
- **0.8.9-dev** : prise en charge OFX/QIF et consolidation de l'anti-doublon
  entre relevés qui se chevauchent. Documenter les variantes prises en charge
  et valider à partir de fixtures synthétiques, sans collecte de vrais relevés.
- Les lots suivants de la série 0.8 sont décalés ; le socle d'import et son
  comportement anti-doublon font partie des critères de validation de 1.0.0.
- Importer un fichier téléchargé manuellement ne nécessite pas de connexion
  de l'application à la banque. La connexion bancaire directe reste un autre lot.

## Parcours d'import prévu

1. Depuis Fonctions de la vue Opérations, ouvrir Importer un relevé. Respecter
   le skill visuel existant ; aucune nouvelle barre permanente dans la liste.
2. Choisir le fichier local et le compte cible du coffre. Retour du sélecteur
   soumis au verrouillage/authentification habituels ; aucun contournement.
3. Pour un CSV, associer les colonnes : date d'opération, éventuelle date de
   valeur, libellé, montant signé OU colonnes débit/crédit, référence bancaire.
   Régler séparateur, encodage, décimales et format des dates selon le fichier ;
   aperçu pour lever les ambiguïtés, pas de date numérique devinée silencieusement.
   Profils réutilisables conservés chiffrés, avec les réglages fonctionnels sauvegardés.
4. Choisir la **date de début d'import, incluse**, au calendrier ; date de fin
   facultative pour borner la plage. Le filtre porte par défaut sur la date
   d'opération définie à l'étape précédente, pas sur la date de lecture du fichier.
5. Afficher l'aperçu avant toute écriture : lignes à ajouter, lignes déjà importées,
   correspondances possibles, erreurs et lignes hors période. Montrer les
   écritures existantes candidates à côté des lignes du relevé (date, libellé,
   montant, état), et les différences pertinentes.
6. Pour chaque rapprochement, proposer ajouter, ignorer ou associer à l'existante.
   Les suggestions ne remplacent pas les écritures existantes automatiquement.
   Aucune donnée n'est enregistrée avant confirmation du récapitulatif.
7. Vérifier une sauvegarde chiffrée du contenu actuel avant validation ; la
   sauvegarde demeure une action distincte, selon le modèle accepté en 0.7.2.
   Si le coffre a changé depuis l'aperçu, recalculer les correspondances avant
   de confirmer, afin de ne pas écraser ou doubler une saisie concurrente.
8. Enregistrer le lot validé et ses références d'import atomiquement. Afficher
   un bilan explicite : ajoutées, associées, ignorées, rejetées et hors période.
   Annuler, une erreur de format ou un échec d'enregistrement conserve l'existant.

## Prévention des doublons

- Comparer à l'historique du **compte cible entier**, pas seulement au mois
  affiché ni à la date de début du fichier : les dates de comptabilisation
  peuvent différer d'une saisie manuelle ou d'une prévision.
- Exploiter une référence bancaire stable si elle existe, avec le compte et
  la provenance appropriés ; documenter le périmètre d'unicité du format.
- En son absence : suggestions sur montant/sens, proximité de date et libellé,
  avec une explication. Même date et même montant ne prouvent pas un doublon.
  Deux dépenses réellement identiques le même jour doivent rester importables.
- Détecter aussi les répétitions à l'intérieur du fichier. Apparier les lignes
  et les écritures une à une : une écriture existante ne doit pas absorber
  plusieurs lignes bancaires distinctes par simple égalité de leurs champs.
- Conserver dans le coffre les références/provenances et décisions utiles pour
  reconnaître la réimportation d'un même fichier et les relevés qui se chevauchent.
  Inclure ces métadonnées dans les sauvegardes/restaurations. Ne pas considérer
  le seul nom du fichier comme identifiant fiable.
- Proposer une association à une saisie manuelle, une occurrence de série
  (y compris prévision/à confirmer) ou un côté de virement déjà enregistré,
  pour éviter de compter deux fois la même dépense. Ne pas créer implicitement
  une seconde paire de virement en important les relevés des deux comptes.
- Préserver les IDs, catégories, annotations et pointages existants par défaut.
  Toute correction de montant/date/état est montrée et explicitement choisie.
  Ne pas modifier les autres occurrences enregistrées ni le modèle futur d'une
  série implicitement ; confirmation et pointage restent distincts.
- Ne pas pointer automatiquement toutes les lignes parce qu'elles proviennent
  d'un relevé. Une éventuelle option de pointage devra être explicite.

## Traitement local et critères de validation

Lecture/analyse locales, données intégrées au coffre chiffré, aucune connexion
bancaire ni permission réseau ajoutée. Pas de copie temporaire du relevé en clair
ni de contenu financier dans les journaux/rapports. Le fichier source appartient
à l'utilisateur ; l'import ne le supprime ni ne prétend le chiffrer sur place.
Les aperçus sensibles sont effacés au verrouillage et relus après authentification,
pas placés dans un état Android sauvegardé en clair.

Tester notamment : limites de dates inclusives, mauvais format/encodage, guillemets
et séparateurs CSV, débit/crédit et centimes exacts, devises et compte cible,
fichier volumineux borné, interruption, annulation, réimportation du même fichier,
relevés qui se chevauchent, deux achats légitimement identiques, libellé bancaire
différent de la saisie, date décalée, rapprochement de séries/virements, restauration
avec conservation des références d'import et absence de modification partielle.

Décision de test utilisateur : aucune livraison 0.7.5-dev technique distincte
ne sera sollicitée. Le travail de stockage est regroupé avec 0.7.6-dev et ses
fonctions visibles ; les contrôles techniques automatisés restent requis.

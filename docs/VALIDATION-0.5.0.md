# Validation 0.5.0-dev

Premier lot periodicite : semaines, quinzaine, mois, fin de mois, 2/3/4/6 mois,
an et deux ans. Fin par date incluse OU nombre total (premiere operation incluse),
sinon sans limite. Debut civil ISO obligatoire : anciennes dates textuelles a
remplacer explicitement. Calendrier ancre sur le jour initial : le 31 reprend
son jour en mars apres le dernier jour de fevrier. Date de fin >= debut.

Schema comptable 5 : regles autonomes, identite de serie, numero d'occurrence et
curseur de prochaine occurrence. Le curseur et les nouvelles lignes (ou paires)
sont sauvegardes ensemble dans une transaction SQLCipher. Generation par lots de
256 occurrences, limite 10 000 lignes / 500 series ; blocage visible en reglages
sans avancer le curseur si stockage insuffisant. Pas de dechiffrement verrouille.
Reprise a l'ouverture et verification chaque minute pendant la session ouverte.
Pas de generation pendant un pointage guide pour ne pas invalider sa selection.

Nouvelle occurrence non pointee ; paire de virement avec deux ids reciproques.
Chaque couple serie/occurrence doit etre unique hors paire liee. Supprimer une
occurrence ne recule jamais le curseur et ne la recree pas. Arreter/reprendre
conserve l'historique, reprise rattrape les echeances dues. Series terminees
restent presentes pour valider les anciennes occurrences et sauvegardes.

Edition depuis une occurrence : par defaut uniquement celle-ci, option pour le
montant/libelle/references des futures echeances sans changer le calendrier.
Un changement de frequence/fin/debut d'une serie en cours demande pour ce premier
lot arret puis nouvelle serie ; projection future et rappels restent differes.
Fin/compte/frequence definis a la creation ; arret/reprise dans Reglages.

Essais manuels dans les nouveautes : rattrapage avec total 3, reouverture sans
doublon, pointage independant, virement periodique, modification isolee/future,
arret/reprise et restauration. Les tests doivent verifier fin de mois, bissextile,
limites, generation atomique et curseur non avance en cas de capacite insuffisante.
Ne pas retrograder sur une ancienne application apres creation de periodicite.

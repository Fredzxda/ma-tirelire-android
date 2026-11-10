# Validation 0.3.5-dev

Date civile ISO pour nouvelles operations et dates explicitement remplacees ;
affichage localise FR/EN. Date du jour fixee au debut du brouillon, conservee
apres rotation et dans le coffre/archive. Pas de remplacement automatique a minuit.
Le calendrier est protege FLAG_SECURE ; l'annuler ne modifie pas le brouillon.

Les anciens libelles localises restent inchanges et ne sont pas interpretes.
Une ancienne operation peut conserver son texte ou recevoir une date explicite.
Un ancien brouillon de nouvelle operation sans date propose aujourd'hui a sa reprise.
Champ de date facultatif dans les anciens brouillons : archives historiques lisibles.
Dates de valeur, tri chronologique et periodicite restent des lots distincts.

Essais : saisie date passee, modification sans doublon ni perte du pointage,
annulation calendrier, rotation, verrouillage, sauvegarde/restauration et date
ancienne en texte preservee. Tester portrait/paysage et les formats FR/EN.

Le pointage avec releve 0.3.4-dev a ete valide par Frederic avant ce lot.

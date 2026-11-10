# Validation 0.5.4-dev

0.5.3-dev validee par Frederic. Petit lot de projection des series actives :
consultation par compte depuis Operations, horizons 30/90/365 jours, dates et
montants localises. Montant oppose selon le cote du virement et autre compte
indique. Les limites date/nombre, arret et recalages de calendrier sont respectes.

Lecture seule, distincte du moteur de generation : pas de mutation du modele,
curseur, stockage, pointage ni solde. Perimetre strictement apres aujourd'hui
et avant/au dernier jour de l'horizon. Les operations deja enregistrees ne sont
pas relistees et la projection ne pretend pas calculer un solde previsionnel
complet (operations ponctuelles, dates de valeur et devises restent hors scope).
Le retard non genere est saute par calcul de periode, pas boucle non bornee.

Tri chronologique global, limite aux 500 premieres echeances avec message si
tronque. Le calendrier de la serie conserve la meme regle de fin de mois et
d'annee bissextile que la generation. Series terminees ou arretees exclues.
Pas de nouveau schema ni migration. FLAG_SECURE et verrouillage conserves.

Tests manuels dans les nouveautes : horizons, arret/reprise, modification future,
virements sur les deux comptes, ouverture repetee sans generation, rotation.
Les rappels, macros et previsions globales demeurent des lots suivants.

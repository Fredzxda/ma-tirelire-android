# Validation 0.3.4-dev

Pointage guide depuis la liste des operations, pour le compte selectionne.
Solde cible signe FR/EN, total propose incluant le solde initial, ecart exact
BigDecimal. Confirmation seulement a ecart nul ; une seule ecriture de snapshot.
Les operations des autres comptes et le solde total restent inchanges.

Selections provisoires en memoire ViewModel : rotation conservee, annulation et
verrouillage abandonnent le travail non confirme. Le pointage manuel reste disponible.
Pas de changement du format de coffre ni d'archive. Historique des releves et
identifiant de releve ne sont pas implementes dans ce lot.

Essais : cible nulle/negative, ecart non nul puis nul, confirmation, annulation,
rotation, verrouillage, conservation apres reouverture et sauvegarde/restauration.
Les tests doivent aussi refuser une confirmation si les operations ou le solde
initial ont change pendant la session.

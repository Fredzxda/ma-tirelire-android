# 0.6.2-dev — espace paysage et pointage

Paysage : navigation laterale meme sur une petite largeur ; nom/version discrets
dans le rail defilant, aucun BrandHeader dans le contenu, soldes fixes compacts.
Portrait : en-tete conserve. Les deux orientations placent le pointage sous le
montant a droite, dans une cible de 48 dp avec semantique Checkbox et action unique.
Les autres zones de la carte ouvrent toujours la fiche. Aucun changement comptable
ni de format du coffre. Version precedente : 2ac89ec ; conserver une sauvegarde
chiffree et ne pas desinstaller pour revenir en arriere.

Essais visuels : portrait/paysage, liste longue, navigation laterale et version,
pointage sans ouvrir la fiche, grands montants/libelles/texte Android agrandi.

Validation locale : compilation des APK application/tests, 97 tests unitaires
et lint reussis. Installation et tests ASUS non realises : cible ADB absente.
Validation visuelle utilisateur encore attendue.

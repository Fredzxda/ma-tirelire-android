# Archivage et suppression des comptes — 0.7.11-dev

## Gestes et confirmation

Sur la page Comptes, glisser une carte à gauche pour archiver, à droite pour
supprimer. Le geste terminé ouvre un dialogue, sans modifier les données.
Annuler conserve le compte ; la carte revient en place. Les mêmes commandes
sont accessibles aux technologies d’assistance. Les confirmations et les
libellés sensibles restent en mémoire, jamais dans un état Android en clair.

## Archivage

L’historique, les soldes et les pointages sont conservés. Le compte apparaît
dans la section Comptes archivés et reste consultable par recherche/période.
Il est exclu du total des comptes actifs, de la nouvelle saisie et des cibles
de virement. Les séries le concernant, y compris les virements, sont suspendues.
Les autres écritures déjà enregistrées restent intactes.

La configuration du compte propose Désarchiver. Cette action ne reprend pas
automatiquement les séries. Elles se gèrent séparément dans les réglages.
Une ancienne occurrence ordinaire ne doit pas permettre de reprendre indirectement
une série dont le modèle vise encore un compte archivé ; un test couvre ce cas.

## Suppression définitive

La confirmation montre le nombre d’opérations effacées, de contreparties
conservées et de séries concernées, ainsi que le solde du compte supprimé.
Après validation, le compte, son solde initial et ses opérations disparaissent.
Les contreparties des autres comptes sont conservées et dissociées : leurs
montants, devises, pointages et soldes ne changent pas. Elles deviennent des
opérations externes prises en compte dans les flux selon leur état.

Les séries concernées sont arrêtées. Lorsqu’une série possède encore des
écritures dans un autre compte, un modèle historique arrêté est conservé pour
préserver les états Prévision/À confirmer/Confirmée de ces écritures ; aucune
prévision ne devient confirmée implicitement. Les modèles sans écriture restante
peuvent disparaître avec le compte supprimé.

Les indices des comptes, devises, restrictions de références, séries et brouillons
sont recalculés ensemble, validés avant application, puis persistés en une fois.
Une référence réservée uniquement au compte supprimé est masquée plutôt que
rendue disponible implicitement partout. Les sauvegardes existantes ne sont pas
supprimées. Il reste possible de restaurer une archive complète volontairement.

**Limite actuelle : le dernier compte du coffre ne peut pas être supprimé.**
Il peut être archivé, ou un autre compte peut être créé avant sa suppression.

## Compatibilité et vérification

L’archivage utilise le schéma 14 ; lecture des anciens schémas 1 à 13 conservée.
Une version antérieure ne sait pas lire une archive contenant le schéma 14.
Conserver une sauvegarde avant une suppression réelle ou un retour à une ancienne
application. Aucun changement du format cryptographique ni des clés Android.

Validation : 157 tests unitaires et lint réussis ; 12 tests isolés sur ASUS réussis,
dont glissements, annulation et confirmation d’archivage/suppression. Le modèle
des tests est fictif : aucun compte personnel n’a été supprimé par le harnais.
Les tests JVM couvrent les contreparties multidevises, les statuts prévisionnels,
le réindexage, les archives et le cas de reprise indirecte d’une série archivée.

Essais utilisateur : annuler un archivage, confirmer puis consulter son historique,
désarchiver via la configuration ; supprimer un compte de test avec un virement
et vérifier les soldes et pointages du compte restant. Essayer portrait/paysage.

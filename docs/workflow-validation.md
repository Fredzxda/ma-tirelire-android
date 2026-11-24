# Tests ciblés — règle corrigée le 24 novembre 2026

La règle précédente « une suite complète à chaque livraison » est supprimée.
Le harnais exige maintenant un choix explicite ; une omission échoue avant
toute connexion ADB. Les commandes -PlanOnly permettent de relire le périmètre.

| Changement | Suite | Tests sur téléphone |
|---|---|---|
| Date suivante des séries | NextDate | 1 test du calendrier |
| Icônes des catégories | Icons | 3 tests des icônes |
| Agencement de saisie | Forms | 5 tests de formulaire |
| Recherche/menus des opérations | Operations | 3 méthodes ciblées |
| Stockage et clés Android | Storage | 3 tests SQLCipher/Keystore |
| Texte/documentation sans comportement | None | Aucun |
| Régression transversale identifiée | Full | Suite complète, justification écrite |

Les tests unitaires sont également filtrés par lot. Adapter ces sélections aux
fichiers réellement touchés ; ne pas utiliser un lot voisin par commodité.
Un nouvel écran ou comportement reçoit sa suite ciblée. Un échec ne déclenche
pas automatiquement Full : diagnostiquer puis rejouer seulement ce qui change.

Exemples :

```powershell
powershell -NoProfile -File tools/play.ps1 -Suite NextDate -PlanOnly
powershell -NoProfile -File tools/play.ps1 -Suite NextDate
powershell -NoProfile -File tools/play.ps1 -Suite None
```

Une modification de format doit couvrir migrations et sauvegardes concernées,
mais ne justifie pas à elle seule de rejouer tous les écrans. Full exige aussi `-FullReason "justification concrète"`. Toute extension
du périmètre est annoncée avec sa raison. Lint et compilation restent locaux.
Les journaux restent sur disque ; bilan et erreurs utiles seulement en chat.
Limiter la délégation, transmettre un contexte minimal et conserver un état
de reprise court. Aucun pourcentage de réduction du quota n’est garanti : le
temps de calcul local et l’usage du modèle sont des coûts distincts.

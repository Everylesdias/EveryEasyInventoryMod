# EveryEasyInventoryMod — conception

> Mod de gestion d'inventaire et de coffres. Nom affiché : **EveryEasyInventoryMod** ;
> mod id : `everyeasyinventorymod` (minuscules imposées par les loaders).
> Statut : **conception** — rien n'est implémenté. Les points marqués **** sont des
> propositions ; ce qui est validé passe dans le journal des décisions en bas.

## 1. Cible technique

| Élément | Valeur | Source |
|---|---|---|
| Minecraft | **26.3** (dernière release officielle ; 26.4 est en snapshot) | manifeste Mojang, 2026-10-29 |
| NeoForge | 26.3.0.99 (dernière stable) | maven.neoforged.net |
| Fabric | Fabric Loader + Fabric API pour 26.3 | meta.fabricmc.net |
| Java | **JDK 25** requis depuis MC 26.1 (la machine a un JDK 21 → à installer) | |

Depuis 26.1, Minecraft n'est plus obfusqué : Fabric et NeoForge voient les mêmes noms de
classes Mojang, ce qui rend le partage de code entre les deux versions beaucoup plus simple.

### Structure proposée (multi-loader)

Un seul dépôt Gradle, trois modules (modèle « MultiLoader-Template », sans dépendance externe
comme Architectury) :

```
common/    toute la logique : tri, transferts, dépôt de zone, hologramme, paquets réseau
fabric/    point d'entrée Fabric, enregistrement touches / paquets / rendu
neoforge/  point d'entrée NeoForge, idem via les événements NeoForge
```

Chaque loader n'a qu'une fine couche d'adaptation ; ~90 % du code vit dans `common`.

### Client / serveur

Le contenu d'un coffre n'est connu que du serveur. Le dépôt de zone et l'hologramme ont donc
besoin du mod **côté serveur** (en solo c'est automatique : le monde tourne sur un serveur
intégré). Proposition :

- mod installé des deux côtés → toutes les fonctions ;
- client seul sur un serveur vanilla → seules les fonctions « dans un écran ouvert » marchent
  (tri, transferts rapides, tout prendre / tout déposer), simulées par des clics normaux ;
  dépôt de zone et hologramme désactivés.

## 2. Fonctions classiques (type Inventory Tweaks)

Disponibles dans l'inventaire du joueur et dans tout écran de conteneur (coffre, tonneau,
boîte de shulker, etc.).

| Fonction | Commande proposée |
|---|---|
| **Trier** le conteneur ou l'inventaire | bouton dans l'écran + clic molette / touche `R` |
| **Tout déposer** (inventaire → conteneur) | bouton ; hotbar exclue par défaut |
| **Déposer ce qui correspond** (seulement les items déjà présents dans le conteneur) | bouton |
| **Tout prendre** (conteneur → inventaire) | bouton |
| **Shift + glisser** : passer la souris sur des cases en tenant Shift + clic pour toutes les transférer | souris |
| **Ctrl + Shift + clic** : transférer tous les items du même type | souris |
| **Molette** sur un item : transférer un par un | souris |

Règles de tri proposées : ordre des onglets créatifs, puis fusion des piles
incomplètes ; la hotbar n'est jamais triée ; armure et main secondaire jamais touchées.

**Remplissage auto de la hotbar** : quand une pile en main se vide ou qu'un outil casse, elle est
remplacée par une pile identique prise dans l'inventaire.

**Cases verrouillées** : le joueur marque des cases de son inventaire (raccourci à définir, par
ex. Alt + clic) ; un cadenas s'affiche et le tri, « tout déposer » et le dépôt de zone ne les
touchent jamais.

## 3. Dépôt de zone (« rangement express »)

**Déclencheur** : un petit bouton **« Envoyer »** dans l'écran d'inventaire du joueur (la touche
`E` garde son comportement vanilla). Le bouton est placé **du côté opposé aux boutons de tri**
pour éviter une fausse manipulation. Proposition d'emplacement : boutons de tri en
haut à droite de la grille, « Envoyer » en haut à gauche.

**Zone** : cube centré sur le joueur, ±5 blocs sur chaque axe (11×11×11 pour qu'il soit
symétrique ; « 10×10 » strict obligerait à décaler le cube d'un côté). Rayon configurable.

**Conteneurs visés** : coffres (toutes variantes, y compris coffres piégés et coffres
en cuivre), tonneaux, boîtes de shulker posées. Exclus : coffre de l'Ender, entonnoirs, fours,
conteneurs d'autres mods (option possible plus tard).

**Algorithme (côté serveur)** :

1. Lister les conteneurs de la zone ; un coffre double compte pour un.
2. Pour chaque pile de l'inventaire concernée (hotbar exclue par défaut, armure et main
   secondaire jamais) :
   1. compléter d'abord les piles **identiques** (même item et mêmes composants) déjà présentes
      dans les conteneurs, du plus proche au plus loin ;
   2. puis, s'il reste des items, les mettre dans une case vide d'un conteneur qui contient
      déjà cet item ;
   3. un item qui n'existe dans aucun conteneur de la zone reste dans l'inventaire.
3. Retour au joueur : son court + message « 143 items rangés dans 4 coffres » ; les coffres
   touchés scintillent brièvement (particules).

Garde-fous : le serveur vérifie que le joueur pourrait ouvrir chaque conteneur (protection de
spawn, mods de claims, coffres verrouillés), et limite la fréquence (pas plus d'un rangement par
seconde).

## 4. Hologramme du contenu d'un coffre

**Quand** : le centre de la vue (réticule) vise un conteneur, à **3 blocs maximum** de l'œil du
joueur. Un seul conteneur à la fois. Disparaît dès que le regard quitte le coffre (petit fondu).

**Quoi** : la liste des items **distincts** du conteneur, chacun affiché **une seule fois** :

- on regroupe par type d'item en ignorant l'état : usure, enchantements, nom, potions, etc. ;
- les variantes d'oxydation et de cire du cuivre sont regroupées sous la forme de base
  (cuivre, cuivre exposé, altéré, oxydé, cirés → une seule case) ;
- icône affichée : la première trouvée dans le coffre ;
- sous chaque icône, le **total** de cet item dans le conteneur (toutes variantes regroupées).

Conteneurs concernés : les mêmes que le dépôt de zone (coffres, tonneaux, shulkers posées).

**Rendu** : panneau flottant au-dessus du coffre, toujours face au joueur (billboard), cases
d'inventaire semi-transparentes façon interface vanilla, 9 colonnes max, autant de lignes que
nécessaire (6 au plus pour un coffre double). Taille fixe à l'écran pour rester lisible.

**Données** : le client demande le résumé au serveur quand il commence à viser un coffre, puis
le serveur pousse les mises à jour tant que le joueur le vise (contenu qui change). Le serveur
vérifie la distance et le droit d'ouvrir le coffre avant de répondre.

## 5. Configuration

Fichier de config par côté (client : affichage, touches ; serveur : rayon, conteneurs autorisés,
hotbar incluse ou non). Écran de config : natif sous NeoForge, via Mod Menu sous Fabric.

## 6. Recherche d'item (en conception, v1.1)

**Demande** : une barre de recherche sous l'inventaire, avec autocomplétion. Une fois un item choisi,
son nom reste dans la barre ; tant qu'elle n'est pas vidée :

- l'item est mis en évidence dans l'inventaire s'il y est ;
- sinon, les conteneurs à **10 blocs max** et dans le champ de vision qui le contiennent sont entourés.

### Qui fait quoi

| Partie | Où | Coût |
|---|---|---|
| Autocomplétion | client seul : liste des ~1 500 items du registre, noms traduits mis en cache une fois par langue | négligeable, rien sur le réseau |
| Mise en évidence dans l'inventaire | client seul : 36 cases comparées à chaque image | négligeable |
| Conteneurs qui ont l'item | **serveur** : le client ne connaît pas le contenu des coffres | voir ci-dessous |
| Contour des conteneurs | client : quelques boîtes dessinées dans le monde | négligeable (plafond de 64 contours) |

### Coût côté serveur (le point sensible)

Le client envoie l'item cherché **une fois** (puis « recherche vidée »). Le serveur garde la recherche
active du joueur et répond avec la liste des positions des conteneurs qui ont l'item.

1. **Parcourir les block entities, pas les blocs.** Un cube de ±10 blocs fait 9 261 positions ; le rangement
   express actuel teste chaque position. Pour la recherche, on parcourt plutôt la table des block entities
   des 9 tronçons (chunks) au plus qui touchent la sphère : on ne regarde que les vrais conteneurs.
   Ordre de grandeur : une salle de stockage dense de 500 coffres × 27 cases = 13 500 comparaisons
   d'item, soit environ 0,1 ms.
2. **Pas à chaque tick.** Nouvelle analyse toutes les 10 ticks (0,5 s) seulement pour les joueurs qui ont
   une recherche active, et seulement s'ils ont bougé d'au moins un bloc ou si un conteneur proche a changé
   (sinon on garde le dernier résultat). Avec 0,1 ms toutes les 10 ticks : 0,01 ms/tick par joueur,
   donc 0,5 ms/tick pour 50 joueurs qui cherchent tous en même temps dans des salles pleines
   (sur un budget de 50 ms). Les analyses des joueurs sont étalées sur les 10 ticks.
3. **Paquets seulement quand le résultat change** (empreinte, comme l'hologramme) ; une liste de positions
   fait quelques dizaines d'octets.
4. **Jamais de chargement de tronçon** : seuls les tronçons déjà chargés sont lus.
5. **Pas de cache global du contenu des coffres** : un index serait plus rapide à lire mais coûterait à
   chaque modification de chaque coffre du serveur, même quand personne ne cherche. L'analyse à la
   demande ne coûte que quand quelqu'un cherche.
6. **Mêmes garde-fous que l'hologramme** : seulement les conteneurs que le joueur pourrait ouvrir
   (protections, claims) ; les coffres de butin pas encore générés sont ignorés (on ne les génère pas).

Le champ de vision est filtré **côté client** (le serveur renvoie tout ce qui est à 10 blocs, le client ne
dessine que ce qui est devant la caméra) : aucun lancer de rayon côté serveur.

Sans le mod sur le serveur, seules l'autocomplétion et la mise en évidence dans l'inventaire marchent.

### Questions ouvertes

- Contour visible **à travers les murs** ou seulement si le conteneur est directement visible ?
- Recherche par **item exact** ou par **groupe** (comme l'hologramme : tous les cuivres, tous les états
  d'usure) ?
- Chercher aussi **dans les shulkers rangées dans les coffres** ?
- Barre aussi dans les écrans de coffre, ou seulement dans l'inventaire ?
- La recherche reste-t-elle active après avoir quitté le monde ?

## 7. État de l'implémentation (v1.0.0, 2026-10-29)

Tout est codé pour les deux loaders. Le scénario de test en jeu passe sous Fabric
(`./gradlew :fabric:runClientGameTest`) et sous NeoForge (`./gradlew :neoforge:runSelftest`) : rangement
express, case verrouillée (Alt + clic) respectée, hologramme (regroupement du cuivre et totaux), tri, tout
prendre / tout déposer, molette, Ctrl+Maj+clic, Maj+glisser, remplissage de la hotbar.

Écarts et précisions par rapport à la conception :

- **Ordre du tri** : ordre d'enregistrement des items (la liste vanilla, très proche des onglets du mode
  créatif). Les vrais onglets créatifs ne peuvent pas être reconstruits sans risque côté serveur.
- **Tri** : fait par le serveur, donc les boutons de tri n'apparaissent que si le serveur a le mod.
  Les transferts (tout prendre, tout déposer, molette, Maj+glisser, Ctrl+Maj+clic) passent par des clics
  normaux et marchent aussi sur un serveur vanilla.
- **Hologramme** : dessiné dans le HUD à l'endroit où le dessus du conteneur se projette à l'écran ;
  il fait donc toujours face au joueur. Sa taille diminue avec la distance (de 100 % à 50 %).
- **Raccourcis** : `R` trie la partie survolée (pas encore réassignable) ; Alt + clic verrouille ou
  déverrouille une case de l'inventaire ; molette bas/haut = envoyer / ramener un item.
- **Remplissage de la hotbar** : désactivé pendant qu'un conteneur est ouvert (sinon déposer le dernier
  item tenu en main le remplacerait aussitôt).
- **Configuration** : `config/everyeasyinventorymod.json` (rayon, hotbar incluse, distance de
  l'hologramme, remplissage, hologramme activé, cases verrouillées). Pas encore d'écran de configuration.

## 8. Journal des décisions

| Date | Décision |
|---|---|
| 2026-10-29 | Cible Minecraft 26.3, versions Fabric et NeoForge. |
| 2026-10-29 | Mod requis des deux côtés ; sur serveur vanilla, seules les fonctions dans un écran ouvert marchent. |
| 2026-10-29 | Dépôt de zone : hotbar exclue par défaut (réglable). |
| 2026-10-29 | Dépôt de zone déclenché par un bouton « Envoyer » dans l'inventaire, à l'opposé des boutons de tri — pas de maintien de `E`. |
| 2026-10-29 | Retenus : remplissage auto de la hotbar, total sous chaque icône de l'hologramme, hologramme et dépôt aussi sur tonneaux et shulkers posées, cases verrouillées. |
| 2026-10-29 | Nom du mod : EveryEasyInventoryMod (mod id `everyeasyinventorymod`). |
| 2026-10-29 | Propositions restantes validées telles quelles : zone ±5 blocs (11×11×11), ordre de rangement (piles identiques puis cases vides d'un conteneur qui a déjà l'item), regroupement de l'hologramme, tri par ordre des onglets créatifs, tri en haut à droite et « Envoyer » en haut à gauche, structure multi-loader sans Architectury. |
| 2026-10-29 | Pas d'écran de configuration (le fichier JSON suffit) ; la touche `R` reste fixe. |
| 2026-10-29 | Licence : LGPL-3.0 (`LGPL-3.0-only`). |

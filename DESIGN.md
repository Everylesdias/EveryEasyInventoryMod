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

## 6. Recherche d'item (v1.1.0)

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
2. **Pas à chaque tick.** Nouvelle analyse seulement pour les joueurs qui ont une recherche active :
   toutes les 10 ticks (0,5 s) s'ils ont changé de bloc, sinon toutes les 20 ticks (1 s) pour voir les
   conteneurs dont le contenu a changé. Avec 0,1 ms toutes les 10 ticks : 0,01 ms/tick par joueur,
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

### Choix validés (2026-10-29)

- **Contour seulement s'il est visible** : dessiné avec le test de profondeur normal, donc caché par les
  blocs devant lui (un coffre au fond d'un mur de coffres n'est entouré que sur ses faces visibles).
  Aucun coût en plus : c'est la carte graphique qui masque.
- **Recherche par groupe**, comme l'hologramme : variantes de cuivre (oxydation, cire) regroupées ; usure,
  enchantements, nom et autres composants ignorés.
- **Dans les shulkers aussi** : un conteneur dont une shulker rangée contient l'item est entouré
  (lecture du composant de contenu, 27 cases de plus par shulker ; pas de récursion plus profonde).
- **Barre sous l'inventaire et sous les écrans de coffre, tonneau et shulker** ; dans un coffre, les
  cases du coffre qui ont l'item sont aussi mises en évidence.
- **Recherche oubliée en quittant** le monde ou le serveur.
- **Mise en évidence dans l'inventaire** : fin cadre lumineux autour des cases qui ont l'item, les autres
  légèrement assombries.
- **Contour jaune doré** dans le monde (même teinte que le cadenas).

## 7. Renommer un conteneur (v1.2.0)

**Demande** : renommer un coffre en cliquant sur son titre (« Coffre ») dans l'écran du coffre ; une fois
renommé, son nom apparaît au-dessus de l'hologramme des items qu'il contient.

### Choix validés (2026-10-29)

- **Nom vanilla du bloc**, le même qu'avec une enclume : affiché en titre à l'ouverture (même pour un joueur
  sans le mod), gardé quand on casse et repose le conteneur. Coffres (les deux moitiés d'un coffre double),
  tonneaux, shulkers posées.
- **Champ à la place du titre** : clic sur le titre → champ de texte ; Entrée valide, Échap annule sans fermer
  le coffre, un clic ailleurs valide ; champ vidé = nom par défaut. Un petit crayon au survol du titre indique
  qu'il est cliquable. 50 caractères au plus (comme l'enclume).
- **Nom au-dessus de l'hologramme, même si le conteneur est vide.**
- **Droits** : qui peut ouvrir le conteneur peut le renommer (mêmes garde-fous que le dépôt et l'hologramme).

### Fonctionnement

Le client envoie la position du conteneur visé à l'ouverture et le nouveau nom ; le serveur vérifie que le
menu ouvert est bien celui de ce conteneur, que le joueur y a accès, nettoie le texte, puis pose le nom.
Nécessite le mod sur le serveur : sans lui, le titre n'est pas cliquable. Coût : un paquet par renommage ;
le nom voyage avec le résumé de l'hologramme déjà existant.

## 8. Réglages et forçage par le serveur (v1.3.0)

**Demande** : choisir la touche de tri dans le menu du jeu ; un écran de réglages (couleur et épaisseur du contour,
contour et recherche activables) ; le serveur peut forcer les réglages des joueurs.

### Choix validés (2026-10-30)

- **Touche de tri** dans Options → Commandes, catégorie Inventaire, R par défaut. Remplace la décision « touche fixe ».
- **Accès à l'écran** : bouton ⚙ dans l'inventaire + liste des mods (native sous NeoForge, Mod Menu facultatif sous Fabric).
- **Couleur** : palette de 8 couleurs + champ #RRGGBB. **Épaisseur** : curseur de 1 à 5.
- **Contenu** : tous les réglages du mod (9), pour ne plus avoir à éditer le JSON. Remplace « pas d'écran de configuration ».
- **Forçage** : fichier serveur et commandes `/eei force|free|list|reload` (opérateurs). Réglage par réglage : libre ou
  forcé. Un réglage forcé est grisé avec un cadenas, et la valeur du joueur est conservée pour les autres serveurs.

### Fonctionnement

- `EeiSetting` décrit chaque réglage (clé, type, bornes). Le client envoie ses réglages à la connexion et à chaque
  changement ; le serveur répond avec ceux qu'il force. Valeur appliquée : forcée, sinon celle du client, sinon par défaut.
- Le serveur **applique lui-même** recherche, contour, hologramme, rayon et hotbar d'Envoyer, remplissage : un client
  modifié ne peut pas les contourner. Couleur et épaisseur ne concernent que l'affichage du client.
- Contour désactivé : le client ne demande plus de recherche dans le monde, donc aucun coût serveur ; la mise en
  évidence dans l'inventaire reste active.
- Envoyer parcourt désormais les block entities des tronçons chargés, comme la recherche, au lieu de tester chaque position.

## 9. Remplir et conteneurs d'autres mods (v1.4.0)

### Choix validés (2026-10-30)

- **Remplir** complète les piles entamées de l'inventaire et de la hotbar depuis les conteneurs à portée d'Envoyer (même
  rayon, même délai). Cases vides et verrouillées intactes.
- **Boutons à icônes** pour Envoyer et Remplir, pour éviter les libellés longs : flèche et coffre, détail dans l'infobulle.
- **Conteneurs d'autres mods** : détection automatique + listes `extraContainers` et `excludedContainers` dans le fichier
  serveur (`mod:bloc` ou `mod:*`), transmises au client.
- **Fonctions** : toutes si possible. Écrans : seulement si le menu ressemble à un coffre. Renommage : si nom vanilla.

### Fonctionnement

- Stockage reconnu : block entity qui implémente `Container`, au moins 9 cases, ni `WorldlyContainer` (machines) ni
  distributeur ni crafter. Limite : un mod qui n'expose que les capacités NeoForge ou la Transfer API Fabric n'est pas
  détecté, et un écran à cases personnalisées ne reçoit pas les boutons.
- Écran comme un coffre : toutes les cases hors joueur sont de simples `Slot` d'un même inventaire, au moins 9.

## 10. État de l'implémentation (v1.4.0, 2026-10-30)

Tout est codé pour les deux loaders. Le scénario de test en jeu passe sous Fabric
(`./gradlew :fabric:runClientGameTest`) et sous NeoForge (`./gradlew :neoforge:runSelftest`) : rangement
express, case verrouillée (Alt + clic) respectée, hologramme (regroupement du cuivre et totaux), tri, tout
prendre / tout déposer, molette, Ctrl+Maj+clic, Maj+glisser, remplissage de la hotbar, recherche d'item
(autocomplétion, E sans effet pendant la saisie, cadre dans l'inventaire, seul le coffre proche dont une shulker
a l'item est trouvé, résultat mis à jour quand il est vidé, barre vidée = recherche arrêtée), renommage
(clic sur le titre, Entrée, nom vanilla posé par le serveur, Échap annule sans fermer, nom au-dessus de
l'hologramme même coffre vide, champ vidé = nom par défaut), réglages (bouton ⚙, touche de tri réassignée, couleur,
épaisseur, contour désactivé) et forçage (`/eei force`/`free`/`reload`, champ grisé, recherche, hologramme et rayon
refusés). `./gradlew :neoforge:runGameTestServer` vérifie que le mod se charge sur un serveur dédié. Validé en jeu par
l'utilisateur le 2026-10-30 : client Fabric avec shaders et Mod Menu sur un serveur dédié Fabric local (localhost), forçage
et persistance après redémarrage compris (v1.3.0). La v1.4.0 ajoute au scénario Remplir, les listes et, sous NeoForge,
un bloc de test qui n'expose que `Container` ; le test `EeiDedicatedServerGameTest` connecte un vrai client à un vrai serveur dédié et écrit un rapport de compatibilité.
Premier résultat ([docs/compat_report_assorted_storage.txt](docs/compat_report_assorted_storage.txt)) : Assorted Storage 13.0.0 n'est **pas** reconnu, ses blocs n'exposent pas `Container`.

Écarts et précisions par rapport à la conception :

- **Ordre du tri** : ordre d'enregistrement des items (la liste vanilla, très proche des onglets du mode
  créatif). Les vrais onglets créatifs ne peuvent pas être reconstruits sans risque côté serveur.
- **Tri** : fait par le serveur, donc les boutons de tri n'apparaissent que si le serveur a le mod.
  Les transferts (tout prendre, tout déposer, molette, Maj+glisser, Ctrl+Maj+clic) passent par des clics
  normaux et marchent aussi sur un serveur vanilla.
- **Hologramme** : dessiné dans le HUD à l'endroit où le dessus du conteneur se projette à l'écran ;
  il fait donc toujours face au joueur. Sa taille diminue avec la distance (de 100 % à 50 %).
- **Raccourcis** : la touche de tri (`R` par défaut) trie la partie survolée ; Alt + clic verrouille ou
  déverrouille une case de l'inventaire ; molette bas/haut = envoyer / ramener un item.
- **Remplissage de la hotbar** : désactivé pendant qu'un conteneur est ouvert (sinon déposer le dernier
  item tenu en main le remplacerait aussitôt).
- **Recherche** : barre sous l'image de l'écran (au-dessus si elle n'a pas la place), suggestions au-dessus
  ou au-dessous selon la place, 6 au plus (le nom qui commence par le texte d'abord, puis un mot du nom, puis
  n'importe où, puis l'identifiant). Modifier le texte après un choix annule la recherche jusqu'au prochain
  choix. Contour limité à 64 conteneurs et à 12 blocs de la caméra côté client.
- **Renommage** : le conteneur est celui que le joueur visait à l'ouverture de l'écran ; ouvert autrement
  (commande, autre mod), le titre n'est pas cliquable. Le titre de l'écran ouvert change tout de suite.
- **Clics** : les raccourcis prennent la case sous la position du clic (et non la case survolée à l'image
  précédente), comme la molette et Maj+glisser.
- **Configuration** : `config/everyeasyinventorymod.json`, modifiable depuis l'écran de réglages (voir §8) ; les
  cases verrouillées y sont aussi enregistrées.

## 11. Journal des décisions

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
| 2026-10-29 | Cases verrouillées : petit cadenas dans le coin haut droit, sans teinte. |
| 2026-10-29 | Recherche d'item (v1.1) : contour seulement si visible, jaune doré ; recherche par groupe, shulkers rangées comprises ; barre dans l'inventaire et les écrans de conteneur ; oubliée en quittant ; cadre + assombrissement dans l'inventaire ; analyse à la demande côté serveur (block entities des tronçons chargés, toutes les 10 ticks, seulement si recherche active). |
| 2026-10-29 | Renommer un conteneur (v1.2) : nom vanilla du bloc, champ à la place du titre (Entrée / Échap / vide = défaut), nom au-dessus de l'hologramme même vide, droits = qui peut l'ouvrir. |
| 2026-10-30 | Réglages (v1.3) : touche de tri dans Options → Commandes, écran avec tous les réglages (bouton ⚙ + liste des mods), couleur par palette + #RRGGBB, épaisseur 1-5 ; serveur : fichier et commandes `/eei`, réglage par réglage, grisé avec cadenas. Remplace « pas d'écran de configuration, touche R fixe ». |
| 2026-10-30 | Remplir (v1.4) : compléter les piles entamées, hotbar comprise ; boutons Envoyer et Remplir à icônes. Conteneurs d'autres mods : détection automatique + listes serveur, toutes les fonctions si possible. |

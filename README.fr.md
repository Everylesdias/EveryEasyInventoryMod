<p align="center"><img src="docs/icon/icon_512.png" width="128" alt="Icône d'EveryEasyInventoryMod"></p>

# EveryEasyInventoryMod

Mod de gestion d'inventaire et de coffres pour Minecraft 26.3, en version Fabric et NeoForge.
*[English version](README.md)* — conception et journal des décisions : [DESIGN.md](DESIGN.md) ;
publication : [docs/publication/PUBLIER.md](docs/publication/PUBLIER.md) ; historique : [CHANGELOG.md](CHANGELOG.md).

## Fonctions

| Où | Quoi | Comment |
|---|---|---|
| Inventaire | Rangement express vers les coffres, tonneaux et shulkers à ±5 blocs qui contiennent déjà l'item | bouton **Envoyer** (en haut à gauche) |
| Inventaire, coffres | Trier | bouton **⇅** ou touche de tri (**R** par défaut, à changer dans Options → Commandes) sur la partie survolée |
| Coffres | Tout prendre / tout déposer / déposer ce qui est déjà présent | boutons **↓**, **↑**, **⇡** |
| Partout | Envoyer toutes les piles survolées | **Maj + glisser** |
| Partout | Envoyer tous les items du même type | **Ctrl + Maj + clic** |
| Coffres | Envoyer / ramener un item | **molette** bas / haut |
| Inventaire | Verrouiller une case (le tri et les dépôts n'y touchent pas) | **Alt + clic** |
| En jeu | Hologramme du contenu du conteneur visé (≤ 3 blocs) | regarder le coffre |
| En jeu | Remplacer la pile en main quand elle s'épuise ou que l'outil casse | automatique |
| Coffres, tonneaux, shulkers | Renommer le conteneur (nom vanilla, comme l'enclume) ; le nom s'affiche au-dessus de l'hologramme | clic sur le **titre**, Entrée valide, Échap annule, vide = nom par défaut |
| Inventaire, coffres | Chercher un item : il est encadré dans l'inventaire, et les conteneurs visibles à 10 blocs qui l'ont (shulkers rangées comprises) sont entourés | barre **Rechercher un item…** sous l'écran, autocomplétion (↑ ↓ Entrée) ; vider la barre arrête la recherche |

Le mod doit être installé sur le serveur pour le rangement express, le tri, l'hologramme, le
remplissage de la hotbar, le renommage et le contour des conteneurs trouvés par la recherche (en solo, c'est automatique).

## Réglages

Bouton **⚙** dans l'inventaire, ou liste des mods (NeoForge, ou Fabric avec Mod Menu) : barre de recherche, contour
des conteneurs avec sa couleur et son épaisseur, hologramme et sa distance, rayon d'Envoyer, hotbar comprise dans
Envoyer, remplissage de la main. Enregistrés dans `config/everyeasyinventorymod.json`.

### Forcer les réglages sur un serveur

Chaque réglage est libre ou forcé à une valeur. Un réglage forcé est grisé avec un cadenas pour les joueurs, et le
serveur refuse lui-même la recherche, l'hologramme et Envoyer au-delà de ce qu'il autorise (la couleur et l'épaisseur
ne concernent que l'affichage).

```
/eei force searchEnabled false
/eei force outlineColor #FF0000
/eei free searchEnabled
/eei list
/eei reload
```

Commandes réservées aux opérateurs, enregistrées dans `config/everyeasyinventorymod-server.json` :

```json
{ "forced": { "searchEnabled": "false", "outlineColor": "#FF0000" } }
```

Clés : `searchEnabled`, `outlineEnabled`, `outlineColor`, `outlineThickness` (1-5), `hologramEnabled`,
`hologramMaxDistance` (1-6), `depositRadius` (1-16), `depositIncludesHotbar`, `hotbarRefill`.

## Construire

Il faut un JDK 25 (Gradle le trouve dans `%USERPROFILE%\.jdks`).

```bash
./gradlew build
```

Les jars sont dans `fabric/build/libs` et `neoforge/build/libs`.

## Tester

```bash
./gradlew :fabric:runClientGameTest
```

Lance un vrai client Fabric, crée un monde et vérifie tout le scénario ; les captures sont dans
`fabric/build/run/clientGameTest/screenshots`.

```bash
./gradlew :neoforge:runSelftest
```

Même scénario dans un vrai client NeoForge : le client crée un monde plat, joue le scénario et se ferme.
Le verdict (`OK` ou la vérification en échec) est dans `neoforge/runs/selftest/eei_selftest_result.txt`,
les captures dans `neoforge/runs/selftest/screenshots`.

Pour jouer en développement : `./gradlew :fabric:runClient` ou `./gradlew :neoforge:runClient`.

## Licence

[LGPL-3.0](LICENSE) (texte de la GPL-3.0 à laquelle elle renvoie : [COPYING](COPYING)).
Vous pouvez utiliser, modifier et redistribuer le mod ; les modifications de son code doivent rester sous LGPL-3.0.

# EveryEasyInventoryMod

Mod de gestion d'inventaire et de coffres pour Minecraft 26.3, en version Fabric et NeoForge.
La conception et le journal des décisions sont dans [DESIGN.md](DESIGN.md).

## Fonctions

| Où | Quoi | Comment |
|---|---|---|
| Inventaire | Rangement express vers les coffres, tonneaux et shulkers à ±5 blocs qui contiennent déjà l'item | bouton **Envoyer** (en haut à gauche) |
| Inventaire, coffres | Trier | bouton **⇅** ou touche **R** sur la partie survolée |
| Coffres | Tout prendre / tout déposer / déposer ce qui est déjà présent | boutons **↓**, **↑**, **⇡** |
| Partout | Envoyer toutes les piles survolées | **Maj + glisser** |
| Partout | Envoyer tous les items du même type | **Ctrl + Maj + clic** |
| Coffres | Envoyer / ramener un item | **molette** bas / haut |
| Inventaire | Verrouiller une case (le tri et les dépôts n'y touchent pas) | **Alt + clic** |
| En jeu | Hologramme du contenu du conteneur visé (≤ 3 blocs) | regarder le coffre |
| En jeu | Remplacer la pile en main quand elle s'épuise ou que l'outil casse | automatique |

Le mod doit être installé sur le serveur pour le rangement express, le tri, l'hologramme et le
remplissage de la hotbar (en solo, c'est automatique).

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

<p align="center"><img src="docs/icon/icon_512.png" width="128" alt="EveryEasyInventoryMod icon"></p>

# EveryEasyInventoryMod

Inventory and chest management for Minecraft 26.3, for **Fabric** and **NeoForge**.
*[Version française](README.fr.md)*

## Features

| Where | What | How |
|---|---|---|
| Inventory | Send items to the nearby chests, barrels and shulker boxes (±5 blocks) that already contain them | **Send** button (top left) |
| Inventory, chests | Sort | **⇅** button, or the sort key (default **R**, change it in Options → Controls) over the part to sort |
| Chests | Take all / deposit all / deposit only what the chest already has | **↓**, **↑**, **⇡** buttons |
| Everywhere | Move every stack you drag over | **Shift + drag** |
| Everywhere | Move every item of the same type | **Ctrl + Shift + click** |
| Chests | Move one item / bring one back | **scroll** down / up |
| Inventory | Lock a slot: sorting and deposits leave it alone | **Alt + click** |
| In world | Hologram of the container you look at (≤ 3 blocks), one slot per item type with its total | look at the chest |
| In world | Refill the hand when a stack runs out or a tool breaks | automatic |
| Chests, barrels, shulkers | Rename the container (vanilla name, like an anvil); the name shows above the hologram | click the **title**, Enter to confirm, Esc to cancel, empty = default name |
| Inventory, chests | Search an item: matching slots are framed, and visible containers within 10 blocks that hold it (shulker boxes inside included) are outlined | **Search an item…** bar below the screen, with autocompletion (↑ ↓ Enter); clear it to stop |

The hologram, search and Send group item variants together (all copper oxidation and wax states, every
durability or enchantment of the same tool).

### Server

Install the mod on both sides for every feature (single player works out of the box). On a server without the
mod, the in-screen features still work: take all, deposit all, scroll, Shift + drag, Ctrl + Shift + click,
locked slots and highlighting in the inventory.

Server-side work is kept small for servers with many chests: searches and holograms only read the containers
of already loaded chunks near the players who use them, a few times per second at most, and packets are sent
only when the result changes. Every action checks that the player may open the container (spawn protection,
claim mods, locked containers).

## Install

- **Fabric**: Fabric Loader 0.19.5+ and [Fabric API](https://modrinth.com/mod/fabric-api).
- **NeoForge**: NeoForge 26.3.
- Java 25 (the default for Minecraft 26.3).

## Settings

Open them with the **⚙** button in the inventory, or from the mod list (NeoForge, or Fabric with
[Mod Menu](https://modrinth.com/mod/modmenu)): search bar, container outline with its color and thickness, hologram
and its distance, Send radius, Send hotbar too, hand refill. They are saved in `config/everyeasyinventorymod.json`.

### Forcing settings on a server

Each setting is either free or forced to a value. Forced settings are greyed out with a padlock for players, and
the server itself refuses search, hologram and Send beyond what it allows (color and thickness are display only).

```
/eei force searchEnabled false
/eei force outlineColor #FF0000
/eei free searchEnabled
/eei list
/eei reload
```

The commands require operator rights and are saved in `config/everyeasyinventorymod-server.json`:

```json
{ "forced": { "searchEnabled": "false", "outlineColor": "#FF0000" } }
```

Keys: `searchEnabled`, `outlineEnabled`, `outlineColor`, `outlineThickness` (1-5), `hologramEnabled`,
`hologramMaxDistance` (1-6), `depositRadius` (1-16), `depositIncludesHotbar`, `hotbarRefill`.

## Build

Requires JDK 25.

```bash
./gradlew build
```

Jars are written to `fabric/build/libs` and `neoforge/build/libs`.

## Test

```bash
./gradlew :fabric:runClientGameTest
```

```bash
./gradlew :neoforge:runSelftest
```

Each one starts a real client, creates a world and plays the whole scenario. The NeoForge verdict is written
to `neoforge/runs/selftest/eei_selftest_result.txt`.

## License

[LGPL-3.0](LICENSE) (it builds on the GPL-3.0, included as [COPYING](COPYING)).
You may use, modify and redistribute the mod; changes to its code must stay under the LGPL-3.0.

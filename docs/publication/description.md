**Sort, move, find and label your items without opening every chest.**

EveryEasyInventoryMod brings the classic inventory tweaks together with a few new tools for big storage rooms.

## Inventory and chests

- **Sort** with one button (⇅) or a key of your choice (default **R**), over the inventory or the chest.
- **Take all / deposit all / deposit only what the chest already has** (↓ ↑ ⇡).
- **Shift + drag** to move every stack you pass over, **Ctrl + Shift + click** to move every item of the same type,
  **scroll** to move items one by one.
- **Lock slots** with **Alt + click**: sorting and deposits leave them alone.
- **Hand refill**: when the stack in your hand runs out or your tool breaks, a matching stack takes its place.

## Send to nearby chests

One click on **Send** in your inventory puts your items into the chests, barrels and shulker boxes within
5 blocks that already contain them, topping up existing stacks first. Your hotbar and locked slots stay put.

**Fill** does the opposite: it tops up your partial stacks from those same containers.

## Containers from other mods

Storage blocks from other mods are recognized automatically, machines excluded, and server owners can add or exclude
blocks.

## Content hologram

Look at a chest, barrel or shulker box from up to 3 blocks away: a small hologram shows each item type it holds
with its total. Variants are grouped (every copper oxidation and wax state, any durability or enchantment).

## Item search

Type in the **search bar** below your inventory, pick an item from the suggestions, and:

- matching slots are framed in your inventory and in the open chest;
- every visible container within 10 blocks that holds it gets a gold outline, even when the item is inside a
  shulker box stored in that chest.

## Rename containers

Click the **title** of a chest, barrel or shulker box screen to rename it. It is the vanilla name (like an
anvil), kept when the block is broken, and it shows above the content hologram.

## Settings

The **⚙** button in your inventory opens the settings: search bar, outline color and thickness, hologram, Send radius,
hand refill and more.

## Server friendly

- Install on both sides for every feature. On a server without the mod, the in-screen tools still work.
- **Server owners can force any setting** with `/eei force` or a config file: forced settings are greyed out for
  players, and the server refuses search, hologram and Send beyond what it allows.
- Built for servers with many chests: searches and holograms only read loaded containers near the players using
  them, a few times per second at most, and send packets only when something changes.
- Every action checks that the player may open the container (spawn protection, claim mods, locked containers).

Available for **Fabric** (requires Fabric API) and **NeoForge**, Minecraft 26.3.

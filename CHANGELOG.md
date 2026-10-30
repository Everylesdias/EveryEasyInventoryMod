# Changelog

## 1.4.0

- New **Fill** button: tops up your partial stacks (hotbar included) from nearby containers. Locked and empty slots
  are left alone. Same radius as Send.
- **Send** and **Fill** are now small icon buttons: an arrow going into or out of a chest.
- Storage blocks from other mods are recognized automatically, through the standard inventory, the Fabric Transfer API
  or NeoForge capabilities: 9 slots or more, or high-capacity slots like crates, machines excluded. They work with Send,
  Fill, the hologram and search; chest-like screens also get the buttons, sorting and the search bar, and renaming when
  the block supports a vanilla name. Tested with Assorted Storage on Fabric.
- Locks from other mods are respected when the mod refuses to give items out of a locked block: Send, Fill, the
  hologram and search then skip it. Tested with Assorted Storage's locks on Fabric.
- Servers can add or exclude blocks with `extraContainers` and `excludedContainers` in
  `config/everyeasyinventorymod-server.json` (`mod:block` or `mod:*`).

## 1.3.0

- The sort key can be changed in Options → Controls (default: R).
- New settings screen, from the ⚙ button in the inventory or the mod list (NeoForge, or Fabric with Mod Menu):
  search bar on/off, container outline on/off, outline color (palette or #RRGGBB) and thickness, hologram on/off and
  distance, Send radius, Send hotbar too, hand refill.
- Servers can force any setting, in `config/everyeasyinventorymod-server.json` or with `/eei force`, `/eei free`,
  `/eei list` and `/eei reload` (operators). Forced settings are greyed out with a padlock for players, and the
  server refuses search, hologram and Send beyond what it allows.
- Send now reads only the containers of loaded chunks instead of testing every block in its range.

## 1.2.0

- Rename chests, barrels and shulker boxes by clicking the title of their screen (vanilla name, like an anvil).
  Enter confirms, Esc cancels, an empty name restores the default one.
- The container name is shown above its content hologram, even when it is empty.
- Search outline is thicker, with a dark edge, so it stays visible in bright light and with shaders.
- Click shortcuts now use the slot under the click instead of the slot hovered on the previous frame.

## 1.1.0

- Item search bar below the inventory and container screens, with autocompletion.
- Matching slots are framed, others are dimmed; shulker boxes holding the item count too.
- Visible containers within 10 blocks that hold the item get a gold outline.
- Locked slots now show a small padlock instead of a red tint.
- Buttons follow the screen when the recipe book is open.

## 1.0.0

- Sort, take all, deposit all, deposit matching; Shift + drag, Ctrl + Shift + click, scroll to move one item.
- Send button: deposit into nearby chests, barrels and shulker boxes that already contain the item.
- Content hologram above the container you look at.
- Locked slots (Alt + click) and automatic hand refill.

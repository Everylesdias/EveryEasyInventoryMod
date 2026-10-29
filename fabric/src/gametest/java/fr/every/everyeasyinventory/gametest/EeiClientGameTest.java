package fr.every.everyeasyinventory.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import fr.every.everyeasyinventory.client.ClientState;
import fr.every.everyeasyinventory.client.EeiScreenAccess;
import fr.every.everyeasyinventory.network.Payloads;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Scénario de bout en bout dans un vrai client avec un monde solo :
 * rangement express, cases verrouillées, hologramme, tri, tout prendre / tout déposer, remplissage de la hotbar, recherche d'item.
 * Lancer avec {@code ./gradlew :fabric:runClientGameTest} ; les captures sont dans fabric/build/run/clientGameTest/screenshots.
 */
public class EeiClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            TestServerContext server = world.getServer();
            world.getConnection().waitForChunksRender();
            server.runCommand("time set day");

            BlockPos feet = server.computeOnServer(s -> player(s).blockPosition());
            BlockPos chestA = feet.offset(2, 0, 0);
            BlockPos barrel = feet.offset(-2, 0, 0);
            BlockPos farChest = feet.offset(8, 0, 0);

            setBlock(server, chestA, "chest");
            setBlock(server, barrel, "barrel");
            setBlock(server, farChest, "chest");
            putInBlock(server, chestA, 5, "cobblestone 10");
            putInBlock(server, chestA, 8, "copper_block 1");
            putInBlock(server, chestA, 12, "exposed_copper 1");
            putInBlock(server, chestA, 20, "waxed_oxidized_copper 1");
            putInBlock(server, chestA, 25, "iron_sword[damage=100] 1");
            putInBlock(server, barrel, 0, "dirt 5");
            putInBlock(server, farChest, 0, "iron_ingot 1");

            server.runCommand("clear @a");
            putInPlayer(server, "hotbar.0", "cobblestone 5");
            putInPlayer(server, "inventory.0", "cobblestone 64"); // case 9
            putInPlayer(server, "inventory.1", "dirt 32");        // case 10
            putInPlayer(server, "inventory.2", "iron_ingot 5");   // case 11
            putInPlayer(server, "inventory.3", "stick 10");       // case 12
            putInPlayer(server, "inventory.4", "dirt 10");        // case 13, verrouillée
            context.waitTicks(5);
            context.runOnClient(mc -> {
                if (!ClientState.isLocked(13)) {
                    ClientState.toggleLock(13);
                }
            });

            // ---- Rangement express depuis l'inventaire
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitForScreen(InventoryScreen.class);
            context.waitTicks(2);
            context.takeScreenshot("eei_1_inventaire_boutons");
            context.clickScreenButton("Send");
            context.waitTicks(10);

            server.runOnServer(s -> {
                Inventory inv = player(s).getInventory();
                check(inv.getItem(0).is(Items.COBBLESTONE) && inv.getItem(0).getCount() == 5, "la hotbar ne doit pas bouger");
                check(inv.getItem(9).isEmpty(), "la cobblestone de la case 9 doit partir dans le coffre");
                check(inv.getItem(10).isEmpty(), "la terre de la case 10 doit partir dans le tonneau");
                check(inv.getItem(11).is(Items.IRON_INGOT), "le coffre aux lingots est hors de portée");
                check(inv.getItem(12).is(Items.STICK), "aucun conteneur n'a de bâtons");
                check(inv.getItem(13).is(Items.DIRT) && inv.getItem(13).getCount() == 10, "la case verrouillée ne doit pas bouger");
                check(count(container(s, chestA), Items.COBBLESTONE) == 74, "le coffre doit avoir 74 cobblestones");
                check(count(container(s, barrel), Items.DIRT) == 37, "le tonneau doit avoir 37 terres");
                check(count(container(s, farChest), Items.IRON_INGOT) == 1, "le coffre lointain ne doit pas changer");
            });

            // ---- Hologramme
            context.setScreen(() -> null);
            context.getInput().lookAt(chestA);
            context.waitFor(mc -> ClientState.contents() != null, 100);
            context.waitTicks(5);
            context.takeScreenshot("eei_2_hologramme");
            Payloads.PeekContents contents = context.computeOnClient(mc -> ClientState.contents());
            check(contents.icons().size() == 3, "3 types attendus (cobblestone, cuivre regroupé, épée) : " + contents.icons());
            check(contents.icons().get(0).is(Items.COBBLESTONE) && contents.counts().get(0) == 74, "total de cobblestone : " + contents.counts());
            check(contents.icons().get(1).is(Items.COPPER_BLOCK.weathering().unaffected()) && contents.counts().get(1) == 3, "cuivre regroupé : " + contents.counts());

            // ---- Écran du coffre : tri, tout prendre, tout déposer
            server.runOnServer(s -> player(s).openMenu((MenuProvider) s.overworld().getBlockEntity(chestA)));
            context.waitForScreen(ContainerScreen.class);
            context.waitTicks(2);
            context.takeScreenshot("eei_3_coffre_boutons");
            context.clickScreenButton("⇅");
            context.waitTicks(5);
            server.runOnServer(s -> {
                Container chest = container(s, chestA);
                check(chest.getItem(0).is(Items.COBBLESTONE) && chest.getItem(0).getCount() == 64, "tri : 64 cobblestones en case 0");
                check(chest.getItem(1).is(Items.COBBLESTONE) && chest.getItem(1).getCount() == 10, "tri : 10 cobblestones en case 1");
                int filled = 0;
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    if (!chest.getItem(i).isEmpty()) {
                        check(i == filled, "tri : les items doivent être tassés au début");
                        filled++;
                    }
                }
                check(filled == 6, "tri : 6 piles attendues, " + filled + " trouvées");
            });
            context.takeScreenshot("eei_4_coffre_trie");

            context.clickScreenButton("↓");
            context.waitTicks(5);
            server.runOnServer(s -> check(container(s, chestA).isEmpty(), "tout prendre : le coffre doit être vide"));

            context.clickScreenButton("↑");
            context.waitTicks(5);
            server.runOnServer(s -> {
                Inventory inv = player(s).getInventory();
                check(inv.getItem(13).is(Items.DIRT), "tout déposer : la case verrouillée reste");
                check(inv.getItem(0).is(Items.COBBLESTONE), "tout déposer : la hotbar reste");
                for (int i = 9; i < 36; i++) {
                    check(i == 13 || inv.getItem(i).isEmpty(), "tout déposer : case " + i + " pas vidée");
                }
            });
            // ---- Raccourcis souris (coffre toujours ouvert)
            server.runOnServer(s -> {
                container(s, chestA).clearContent();
                Inventory inv = player(s).getInventory();
                inv.setItem(9, new ItemStack(Items.COBBLESTONE, 10));
                inv.setItem(10, new ItemStack(Items.COBBLESTONE, 10));
                inv.setItem(11, new ItemStack(Items.DIRT, 5));
                inv.setItem(12, new ItemStack(Items.DIRT, 5));
                player(s).containerMenu.broadcastChanges();
            });
            context.waitTicks(5);
            // Menu d'un coffre simple : cases 0-26 pour le coffre, puis 27-53 pour les cases 9-35 de l'inventaire.
            int cobbleA = 27;
            int cobbleB = 28;
            int dirtA = 29;

            hover(context, cobbleA);
            context.getInput().scroll(-1);
            context.waitTicks(5);
            server.runOnServer(s -> {
                check(count(container(s, chestA), Items.COBBLESTONE) == 1, "molette : 1 cobblestone dans le coffre");
                check(player(s).getInventory().getItem(9).getCount() == 9, "molette : 9 cobblestones restent");
            });

            hover(context, dirtA);
            context.getInput().holdControl();
            context.getInput().holdShift();
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.getInput().releaseShift();
            context.getInput().releaseControl();
            context.waitTicks(5);
            server.runOnServer(s -> check(count(container(s, chestA), Items.DIRT) == 10, "Ctrl+Maj+clic : toute la terre part ; coffre = " + count(container(s, chestA), Items.DIRT) + ", case 11 = " + player(s).getInventory().getItem(11) + ", case 12 = " + player(s).getInventory().getItem(12)));

            // Le simulateur n'ajoute pas Maj à l'événement du premier clic (géré par le jeu vanilla) :
            // on part d'une case vide pour ne tester que le glisser du mod.
            hover(context, 35);
            context.getInput().holdShift();
            context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(1);
            hover(context, cobbleA);
            hover(context, cobbleB);
            context.waitTicks(1);
            context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.getInput().releaseShift();
            context.waitTicks(5);
            server.runOnServer(s -> check(count(container(s, chestA), Items.COBBLESTONE) == 20, "Maj+glisser : les deux piles partent ; coffre = " + count(container(s, chestA), Items.COBBLESTONE) + ", case 9 = " + player(s).getInventory().getItem(9) + ", case 10 = " + player(s).getInventory().getItem(10)));

            // Comme Échap : prévient aussi le serveur que le coffre est fermé.
            context.runOnClient(mc -> mc.player.closeContainer());
            context.waitTicks(5);

            // ---- Remplissage auto de la hotbar
            server.runOnServer(s -> {
                Inventory inv = player(s).getInventory();
                inv.setSelectedSlot(1);
                inv.setItem(1, new ItemStack(Items.TORCH, 1));
                inv.setItem(20, new ItemStack(Items.TORCH, 30));
            });
            context.waitTicks(3);
            server.runOnServer(s -> player(s).getInventory().getItem(1).shrink(1));
            context.waitTicks(3);
            server.runOnServer(s -> {
                Inventory inv = player(s).getInventory();
                check(inv.getItem(1).is(Items.TORCH) && inv.getItem(1).getCount() == 30, "remplissage : 30 torches en main ; case 1 = " + inv.getItem(1) + ", case 20 = " + inv.getItem(20) + ", sélection = " + inv.getSelectedSlot() + ", menu = " + player(s).containerMenu);
                check(inv.getItem(20).isEmpty(), "remplissage : la réserve doit être prise");
            });

            // ---- Recherche d'item : un diamant dans l'inventaire, d'autres dans une shulker rangée dans un coffre proche
            BlockPos shulkerChest = feet.offset(0, 0, 3);
            BlockPos farDiamonds = feet.offset(0, 0, -12);
            setBlock(server, shulkerChest, "chest");
            setBlock(server, farDiamonds, "chest");
            putInBlock(server, shulkerChest, 0, "shulker_box[container=[{slot:0,item:{id:\"minecraft:diamond\",count:3}}]] 1");
            putInBlock(server, farDiamonds, 0, "diamond 5");
            putInPlayer(server, "inventory.6", "diamond 1"); // case 15
            context.waitTicks(5);

            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitForScreen(InventoryScreen.class);
            context.waitTicks(2);
            clickSearchBox(context);
            context.getInput().typeChars("Diam");
            context.getInput().pressKey(InputConstants.KEY_E); // ne doit pas fermer l'inventaire
            context.waitTicks(2);
            check(context.computeOnClient(mc -> mc.gui.screen() instanceof InventoryScreen), "taper E dans la barre ne doit pas fermer l'inventaire");
            context.takeScreenshot("eei_5_recherche_suggestions");
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            context.waitTicks(2);
            check(context.computeOnClient(mc -> ClientState.searchItem() == Items.DIAMOND), "Entrée doit choisir le diamant");
            check(context.computeOnClient(mc -> searchBox(mc).getValue()).equals("Diamond"), "le nom choisi reste dans la barre");
            context.takeScreenshot("eei_6_recherche_inventaire");

            context.runOnClient(mc -> mc.player.closeContainer());
            context.waitFor(mc -> !ClientState.searchResults().isEmpty(), 100);
            check(context.computeOnClient(mc -> ClientState.searchResults()).equals(List.of(shulkerChest)),
                    "seul le coffre à la shulker est trouvé : " + context.computeOnClient(mc -> ClientState.searchResults()));
            context.getInput().lookAt(shulkerChest);
            context.waitTicks(5);
            context.takeScreenshot("eei_7_recherche_contour");

            server.runOnServer(s -> container(s, shulkerChest).clearContent());
            context.waitFor(mc -> ClientState.searchResults().isEmpty(), 100);

            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitForScreen(InventoryScreen.class);
            context.waitTicks(2);
            clickSearchBox(context);
            context.getInput().pressKey(InputConstants.KEY_END);
            for (int i = 0; i < "Diamond".length(); i++) {
                context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
            }
            context.waitTicks(2);
            check(context.computeOnClient(mc -> ClientState.searchItem() == null && ClientState.searchMembers().isEmpty()), "barre vidée : plus de recherche");
        }
    }

    private static EditBox searchBox(Minecraft mc) {
        return mc.gui.screen().children().stream()
                .filter(EditBox.class::isInstance).map(EditBox.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("barre de recherche absente"));
    }

    private static void clickSearchBox(ClientGameTestContext context) {
        double[] pos = context.computeOnClient(mc -> {
            EditBox box = searchBox(mc);
            int scale = mc.getWindow().getGuiScale();
            return new double[]{(box.getX() + 4) * scale, (box.getY() + box.getHeight() / 2.0) * scale};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
        check(context.computeOnClient(mc -> searchBox(mc).isFocused()), "un clic doit donner le focus à la barre");
    }

    /** Place le curseur au centre d'une case du menu ouvert. */
    private static void hover(ClientGameTestContext context, int menuSlot) {
        double[] pos = context.computeOnClient(mc -> {
            EeiScreenAccess screen = (EeiScreenAccess) mc.gui.screen();
            Slot slot = screen.eei$menu().slots.get(menuSlot);
            int scale = mc.getWindow().getGuiScale();
            return new double[]{(screen.eei$leftPos() + slot.x + 8) * scale, (screen.eei$topPos() + slot.y + 8) * scale};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.waitTick();
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static Container container(MinecraftServer server, BlockPos pos) {
        return (Container) server.overworld().getBlockEntity(pos);
    }

    private static int count(Container container, Item item) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) {
                total += container.getItem(i).getCount();
            }
        }
        return total;
    }

    private static void setBlock(TestServerContext server, BlockPos pos, String block) {
        server.runCommand("setblock %d %d %d minecraft:%s".formatted(pos.getX(), pos.getY(), pos.getZ(), block));
    }

    private static void putInBlock(TestServerContext server, BlockPos pos, int slot, String item) {
        server.runCommand("item replace block %d %d %d container.%d with minecraft:%s"
                .formatted(pos.getX(), pos.getY(), pos.getZ(), slot, item));
    }

    private static void putInPlayer(TestServerContext server, String slot, String item) {
        server.runCommand("item replace entity @a " + slot + " with minecraft:" + item);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

package fr.every.everyeasyinventory.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import fr.every.everyeasyinventory.client.ClientState;
import fr.every.everyeasyinventory.client.EeiScreenAccess;
import fr.every.everyeasyinventory.config.EeiSetting;
import fr.every.everyeasyinventory.logic.ContainerFinder;
import fr.every.everyeasyinventory.logic.EeiStorage;
import fr.every.everyeasyinventory.logic.ServerActions;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * Un vrai client connecté à un vrai serveur dédié, avec les paquets et les droits d'un joueur normal.
 * Écrit {@code eei_compat_report.txt} dans fabric/build/run/clientGameTest : une ligne de résultats par bloc testé.
 * Les blocs vanilla sont vérifiés ; ceux des autres mods (dossier fabric/test-mods, facultatif) sont seulement rapportés.
 */
public class EeiDedicatedServerGameTest implements FabricClientGameTest {

    /** Blocs de stockage : toutes les fonctions sont attendues sur ceux du jeu. */
    private static final List<String> VANILLA_STORAGE = List.of("minecraft:chest", "minecraft:barrel");
    /** Autres mods : le rapport dit ce qui marche. */
    private static final List<String> OTHER_MODS = List.of(
            "assortedchests:chest_iron", "assortedbarrels:barrel_iron", "assortedcrates:oak_crate", "assortedcrates:oak_crate_double");
    /** Ne doivent jamais être pris pour des stockages. */
    private static final List<String> NOT_STORAGE = List.of("minecraft:furnace", "minecraft:hopper", "minecraft:dropper", "minecraft:brewing_stand");

    @Override
    public void runTest(ClientGameTestContext context) {
        StringBuilder report = new StringBuilder("bloc | reconnu | Envoyer | Remplir | hologramme | recherche | écran | boutons | barre | tri | renommage | classe\n");
        Properties properties = new Properties();
        properties.setProperty("server-port", "25599"); // pas le port par défaut : un vrai serveur peut déjà tourner
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            server.runCommand("time set day");
            server.runCommand("difficulty peaceful");
            BlockPos feet = server.computeOnServer(s -> player(s).blockPosition());
            BlockPos target = feet.offset(2, 0, 0);

            check(context.computeOnClient(mc -> ClientState.serverHasMod()), "le client voit le mod sur le serveur dédié");

            testCommandRights(context, server);
            server.runCommand("op " + server.computeOnServer(s -> player(s).getGameProfile().name()));
            context.waitTicks(5);

            for (String id : VANILLA_STORAGE) {
                String line = testBlock(context, server, id, target);
                report.append(line).append('\n');
                check(!line.contains("NON"), "toutes les fonctions sur " + id + " : " + line);
            }
            for (String id : OTHER_MODS) {
                if (BuiltInRegistries.BLOCK.containsKey(Identifier.parse(id))) {
                    report.append(testBlock(context, server, id, target)).append('\n');
                } else {
                    report.append(id).append(" | mod absent\n");
                }
            }
            report.append("\nverrou posé, joueur sans clé | ouvrir | Envoyer | Remplir | hologramme | recherche\n");
            for (String id : OTHER_MODS) {
                if (BuiltInRegistries.BLOCK.containsKey(Identifier.parse(id))) {
                    String line = testLock(context, server, id, target);
                    report.append(line).append('\n');
                    check(!line.contains("PASSE"), "un joueur sans clé ne doit rien obtenir : " + line);
                }
            }
            report.append('\n');
            for (String id : NOT_STORAGE) {
                setBlock(server, target, id);
                context.waitTicks(3);
                boolean supported = context.computeOnClient(mc -> ContainerFinder.isSupported(mc.level.getBlockEntity(target)));
                report.append(id).append(" | ").append(supported ? "OUI (erreur)" : "exclu").append('\n');
                check(!supported, id + " ne doit pas être un stockage");
            }
        } finally {
            try {
                Files.writeString(Path.of("eei_compat_report.txt"), report.toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Verrou posé par le mod lui-même : ce qu'un joueur sans clé arrive encore à faire. */
    private static String testLock(ClientGameTestContext context, TestDedicatedServerContext server, String id, BlockPos pos) {
        // Regard ailleurs : l'hologramme du bloc précédent ne doit pas compter.
        context.getInput().lookAt(pos.above(5));
        setBlock(server, pos, "minecraft:air");
        context.waitTicks(10);
        setBlock(server, pos, id);
        context.waitTicks(5);
        if (!context.computeOnClient(mc -> ContainerFinder.isSupported(mc.level.getBlockEntity(pos)))) {
            return id + " | non reconnu";
        }
        server.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(9, new ItemStack(Items.GOLD_INGOT, 5));
            inv.setItem(10, new ItemStack(Items.EMERALD, 1));
            storage(s, pos).fillEmpty(new ItemStack(Items.GOLD_INGOT, 1));
            storage(s, pos).fillEmpty(new ItemStack(Items.EMERALD, 10));
            setLock(s.overworld().getBlockEntity(pos), "secret");
        });
        context.getInput().lookAt(pos);
        context.waitTicks(25);

        // Regarder le coffre et le chercher : le serveur ne doit rien révéler.
        boolean hologram = within(context, mc -> ClientState.contents() != null && !ClientState.contents().icons().isEmpty(), 40);
        context.runOnClient(mc -> ClientState.setSearch("Emerald", Items.EMERALD));
        boolean search = within(context, mc -> !ClientState.searchResults().isEmpty(), 40);
        context.runOnClient(mc -> ClientState.setSearch("", null));

        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        boolean opened = within(context, mc -> mc.gui.screen() != null, 30);
        if (opened) {
            context.runOnClient(mc -> mc.player.closeContainer());
            context.waitTicks(3);
        }

        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(2);
        context.clickScreenButton("Send");
        context.waitTicks(10);
        boolean sent = server.computeOnServer(s -> player(s).getInventory().getItem(9).getCount() != 5);
        context.waitTicks(25);
        context.clickScreenButton("Fill");
        context.waitTicks(10);
        boolean filled = server.computeOnServer(s -> player(s).getInventory().getItem(10).getCount() > 1);
        context.runOnClient(mc -> mc.player.closeContainer());
        context.waitTicks(3);

        server.runOnServer(s -> setLock(s.overworld().getBlockEntity(pos), ""));
        return id + " | " + passed(opened) + " | " + passed(sent) + " | " + passed(filled) + " | " + passed(hologram) + " | " + passed(search);
    }

    private static String passed(boolean value) {
        return value ? "PASSE" : "bloqué";
    }

    private static void setLock(BlockEntity blockEntity, String code) {
        try {
            blockEntity.getClass().getMethod("setLockCode", String.class).invoke(blockEntity, code);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("pas de verrou sur " + blockEntity, e);
        }
    }

    /** {@code /eei} tapé par un vrai joueur : refusé sans droits, accepté en opérateur, propagé au client. */
    private static void testCommandRights(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(mc -> mc.player.connection.sendCommand("eei force searchEnabled false"));
        context.waitTicks(10);
        check(context.computeOnClient(mc -> !ClientState.isForced(EeiSetting.SEARCH_ENABLED)), "/eei doit être refusé à un joueur sans droits");

        server.runCommand("op " + server.computeOnServer(s -> player(s).getGameProfile().name()));
        context.waitTicks(5);
        context.runOnClient(mc -> mc.player.connection.sendCommand("eei force searchEnabled false"));
        check(within(context, mc -> ClientState.isForced(EeiSetting.SEARCH_ENABLED), 40), "/eei force doit marcher en opérateur");
        context.runOnClient(mc -> mc.player.connection.sendCommand("eei free searchEnabled"));
        check(within(context, mc -> !ClientState.isForced(EeiSetting.SEARCH_ENABLED), 40), "/eei free doit marcher en opérateur");
        server.runCommand("deop " + server.computeOnServer(s -> player(s).getGameProfile().name()));
        context.waitTicks(5);
    }

    private static String testBlock(ClientGameTestContext context, TestDedicatedServerContext server, String id, BlockPos pos) {
        setBlock(server, pos, id);
        context.waitTicks(5);
        String className = server.computeOnServer(s -> {
            BlockEntity blockEntity = s.overworld().getBlockEntity(pos);
            return blockEntity == null ? "aucune" : blockEntity.getClass().getSimpleName();
        });
        boolean supported = context.computeOnClient(mc -> ContainerFinder.isSupported(mc.level.getBlockEntity(pos)));
        boolean isContainer = server.computeOnServer(s -> ContainerFinder.find(s.overworld(), pos) != null);
        String none = "-";
        String send = none;
        String fill = none;
        String hologram = none;
        String search = none;
        if (supported && isContainer) {
            send = yes(testSend(context, server, pos));
            fill = yes(testFill(context, server, pos));
            hologram = yes(testHologram(context, server, pos));
            search = yes(testSearch(context, server, pos));
        }
        String screen = testScreen(context, server, pos);
        return id + " | " + yes(supported) + " | " + send + " | " + fill + " | " + hologram + " | " + search + " | " + screen + " | " + className;
    }

    private static boolean testSend(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos pos) {
        server.runOnServer(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(9, new ItemStack(Items.GOLD_INGOT, 5));
            clear(s, pos);
            storage(s, pos).fillEmpty(new ItemStack(Items.GOLD_INGOT, 1));
        });
        context.waitTicks(25);
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(2);
        context.clickScreenButton("Send");
        context.waitTicks(10);
        return server.computeOnServer(s -> count(storage(s, pos), Items.GOLD_INGOT) == 6 && player(s).getInventory().getItem(9).isEmpty());
    }

    /** L'écran d'inventaire est encore ouvert. */
    private static boolean testFill(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos pos) {
        server.runOnServer(s -> player(s).getInventory().setItem(10, new ItemStack(Items.GOLD_INGOT, 1)));
        context.waitTicks(25);
        context.clickScreenButton("Fill");
        context.waitTicks(10);
        boolean ok = server.computeOnServer(s -> player(s).getInventory().getItem(10).getCount() == 7 && count(storage(s, pos), Items.GOLD_INGOT) == 0);
        context.runOnClient(mc -> mc.player.closeContainer());
        context.waitTicks(3);
        return ok;
    }

    private static boolean testHologram(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos pos) {
        server.runOnServer(s -> {
            clear(s, pos);
            storage(s, pos).fillEmpty(new ItemStack(Items.EMERALD, 2));
        });
        context.getInput().lookAt(pos);
        return within(context, mc -> ClientState.contents() != null && !ClientState.contents().icons().isEmpty()
                && ClientState.contents().icons().getFirst().is(Items.EMERALD), 60);
    }

    private static boolean testSearch(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos pos) {
        context.runOnClient(mc -> ClientState.setSearch("Emerald", Items.EMERALD));
        boolean found = within(context, mc -> !ClientState.searchResults().isEmpty(), 60);
        context.runOnClient(mc -> ClientState.setSearch("", null));
        return found;
    }

    /** Vrai clic droit sur le bloc, puis ce que l'écran propose : « type | boutons | barre | tri | renommage ». */
    private static String testScreen(ClientGameTestContext context, TestDedicatedServerContext server, BlockPos pos) {
        String none = "-";
        context.getInput().lookAt(pos);
        context.waitTicks(3);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        if (!within(context, mc -> mc.gui.screen() != null, 40)) {
            return "aucun | - | - | - | -";
        }
        context.waitTicks(3);
        String name = context.computeOnClient(mc -> mc.gui.screen().getClass().getSimpleName());
        if (!context.computeOnClient(mc -> mc.gui.screen() instanceof EeiScreenAccess)) {
            context.runOnClient(mc -> mc.player.closeContainer());
            context.waitTicks(3);
            return name + " (non standard) | - | - | - | -";
        }
        context.takeScreenshot("eei_compat_" + name);
        boolean chestLike = context.computeOnClient(mc -> ServerActions.isChestLikeMenu(((EeiScreenAccess) mc.gui.screen()).eei$menu()));
        boolean buttons = context.computeOnClient(mc -> hasButton(mc.gui.screen(), "⇅"));
        boolean bar = context.computeOnClient(mc -> hasEditBox(mc.gui.screen(), "Item search"));
        boolean renameBox = context.computeOnClient(mc -> hasEditBox(mc.gui.screen(), "Rename container"));

        String sort = none;
        boolean isContainer = server.computeOnServer(s -> ContainerFinder.find(s.overworld(), pos) != null);
        if (buttons && isContainer) {
            server.runOnServer(s -> {
                clear(s, pos);
                storage(s, pos).fillEmpty(new ItemStack(Items.IRON_INGOT, 1));
                storage(s, pos).fillEmpty(new ItemStack(Items.GOLD_INGOT, 10));
                storage(s, pos).fillEmpty(new ItemStack(Items.GOLD_INGOT, 10));
                player(s).containerMenu.broadcastChanges();
            });
            context.waitTicks(5);
            context.clickScreenButton("⇅");
            context.waitTicks(5);
            sort = yes(server.computeOnServer(s -> {
                EeiStorage storage = storage(s, pos);
                for (int i = 0; i < storage.size(); i++) {
                    if (storage.get(i).is(Items.GOLD_INGOT)) {
                        return storage.get(i).getCount() == 20;
                    }
                }
                return false;
            }));
        }

        String rename = none;
        if (renameBox) {
            double[] title = context.computeOnClient(mc -> {
                EeiScreenAccess access = (EeiScreenAccess) mc.gui.screen();
                int scale = mc.getWindow().getGuiScale();
                return new double[]{(access.eei$leftPos() + 12) * scale, (access.eei$topPos() + access.eei$titleLabelY() + 4) * scale};
            });
            context.getInput().setCursorPos(title[0], title[1]);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTick();
            context.getInput().typeChars("Stock");
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            context.waitTicks(5);
            rename = yes(server.computeOnServer(s -> {
                Component custom = s.overworld().getBlockEntity(pos) instanceof Nameable nameable ? nameable.getCustomName() : null;
                return custom != null && custom.getString().equals("Stock");
            }));
        }
        context.runOnClient(mc -> mc.player.closeContainer());
        context.waitTicks(3);
        return name + (chestLike ? "" : " (pas comme un coffre)") + " | " + yes(buttons) + " | " + yes(bar) + " | " + sort + " | " + rename;
    }

    private static boolean hasButton(Screen screen, String text) {
        return screen.children().stream().anyMatch(c -> c instanceof AbstractWidget widget && widget.getMessage().getString().equals(text));
    }

    private static boolean hasEditBox(Screen screen, String narration) {
        return screen.children().stream().anyMatch(c -> c instanceof EditBox box && box.getMessage().getString().equals(narration));
    }

    private static boolean within(ClientGameTestContext context, Predicate<Minecraft> condition, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (context.computeOnClient(condition::test)) {
                return true;
            }
            context.waitTick();
        }
        return false;
    }

    private static String yes(boolean value) {
        return value ? "oui" : "NON";
    }

    private static void setBlock(TestDedicatedServerContext server, BlockPos pos, String id) {
        server.runCommand("setblock %d %d %d %s replace".formatted(pos.getX(), pos.getY(), pos.getZ(), id));
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static EeiStorage storage(MinecraftServer server, BlockPos pos) {
        return ContainerFinder.find(server.overworld(), pos).storage();
    }

    /** Vide le stockage par ses propres règles, comme un joueur qui reprendrait tout. */
    private static void clear(MinecraftServer server, BlockPos pos) {
        EeiStorage storage = storage(server, pos);
        Inventory inventory = player(server).getInventory();
        for (int i = 0; i < storage.size(); i++) {
            for (int tries = 0; tries < 64 && !storage.get(i).isEmpty(); tries++) {
                storage.takeInto(storage.get(i).copyWithCount(1), inventory);
            }
        }
    }

    private static int count(EeiStorage storage, Item item) {
        int total = 0;
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).is(item)) {
                total += storage.get(i).getCount();
            }
        }
        return total;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

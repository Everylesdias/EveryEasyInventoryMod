package fr.every.everyeasyinventory.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import fr.every.everyeasyinventory.Constants;
import fr.every.everyeasyinventory.client.ClientState;
import fr.every.everyeasyinventory.client.EeiScreenAccess;
import fr.every.everyeasyinventory.network.Payloads;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Même scénario que le gametest client Fabric, joué dans un vrai client NeoForge avec un monde solo :
 * rangement express, case verrouillée (Alt + clic), hologramme, tri, tout prendre / tout déposer, molette,
 * Ctrl+Maj+clic, Maj+glisser, remplissage de la hotbar.
 * Lancer avec {@code ./gradlew :neoforge:runSelftest} ; le client se ferme à la fin et écrit le verdict dans
 * neoforge/runs/selftest/eei_selftest_result.txt (captures dans neoforge/runs/selftest/screenshots).
 *
 * <p>Les événements souris sont envoyés directement à l'écran ouvert, avec leurs vrais modificateurs.
 */
@Mod(value = "eei_selftest", dist = Dist.CLIENT)
public class EeiSelfTest {

    private static final String WORLD = "eei_selftest";
    private static final AtomicLong TICKS = new AtomicLong();
    private static boolean started;

    public EeiSelfTest() {
        if (Boolean.getBoolean("eei.selftest")) {
            NeoForge.EVENT_BUS.addListener(EeiSelfTest::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        TICKS.incrementAndGet();
        if (!started && Minecraft.getInstance().gui.screen() instanceof TitleScreen) {
            started = true;
            Thread thread = new Thread(EeiSelfTest::run, "EEI self-test");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static void run() {
        Minecraft mc = Minecraft.getInstance();
        String verdict;
        try {
            scenario();
            verdict = "OK";
            Constants.LOG.info("[self-test] scénario réussi");
        } catch (Throwable t) {
            Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
            verdict = "ÉCHEC : " + cause;
            Constants.LOG.error("[self-test] scénario en échec", cause);
        }
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("eei_selftest_result.txt"), verdict);
        } catch (IOException e) {
            Constants.LOG.error("[self-test] verdict non écrit", e);
        }
        mc.execute(mc::stop);
    }

    private static void scenario() throws Exception {
        // ---- Nouveau monde plat, paisible
        client(mc -> {
            deleteWorld(mc.getLevelSource().getBaseDir().resolve(WORLD));
            LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, WorldOptions.testWorldWithRandomSeed(),
                    WorldPresets::createTestWorldDimensions, mc.gui.screen());
        });
        waitFor("monde chargé", mc -> mc.player != null && mc.level != null && mc.gui.screen() == null, 1200);
        waitTicks(40);
        check(onClient(mc -> ClientState.serverHasMod()), "le client doit voir le mod sur le serveur intégré");
        command("time set day");

        BlockPos feet = onServer(s -> player(s).blockPosition());
        BlockPos chestA = feet.offset(2, 0, 0);
        BlockPos barrel = feet.offset(-2, 0, 0);
        BlockPos farChest = feet.offset(8, 0, 0);

        setBlock(chestA, "chest");
        setBlock(barrel, "barrel");
        setBlock(farChest, "chest");
        putInBlock(chestA, 5, "cobblestone 10");
        putInBlock(chestA, 8, "copper_block 1");
        putInBlock(chestA, 12, "exposed_copper 1");
        putInBlock(chestA, 20, "waxed_oxidized_copper 1");
        putInBlock(chestA, 25, "iron_sword[damage=100] 1");
        putInBlock(barrel, 0, "dirt 5");
        putInBlock(farChest, 0, "iron_ingot 1");

        command("clear @a");
        putInPlayer("hotbar.0", "cobblestone 5");
        putInPlayer("inventory.0", "cobblestone 64"); // case 9
        putInPlayer("inventory.1", "dirt 32");        // case 10
        putInPlayer("inventory.2", "iron_ingot 5");   // case 11
        putInPlayer("inventory.3", "stick 10");       // case 12
        putInPlayer("inventory.4", "dirt 10");        // case 13, verrouillée
        waitTicks(5);

        // ---- Inventaire : Alt + clic verrouille la case 13 (case 13 du menu = case 13 de l'inventaire)
        client(mc -> {
            if (ClientState.isLocked(13)) {
                ClientState.toggleLock(13);
            }
            mc.gui.setScreen(new InventoryScreen(mc.player));
        });
        waitFor("inventaire ouvert", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(2);
        clickSlot(13, InputConstants.MOD_ALT);
        waitTicks(2);
        check(onClient(mc -> ClientState.isLocked(13)), "Alt + clic doit verrouiller la case 13");
        screenshot("eei_1_inventaire_boutons");

        // ---- Rangement express
        clickButton("Send");
        waitTicks(10);
        server(s -> {
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
        client(mc -> {
            mc.player.closeContainer();
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(chestA));
        });
        waitFor("résumé du coffre visé", mc -> ClientState.contents() != null, 100);
        waitTicks(5);
        screenshot("eei_2_hologramme");
        Payloads.PeekContents contents = onClient(mc -> ClientState.contents());
        check(contents.icons().size() == 3, "3 types attendus (cobblestone, cuivre regroupé, épée) : " + contents.icons());
        check(contents.icons().get(0).is(Items.COBBLESTONE) && contents.counts().get(0) == 74, "total de cobblestone : " + contents.counts());
        check(contents.icons().get(1).is(Items.COPPER_BLOCK.weathering().unaffected()) && contents.counts().get(1) == 3, "cuivre regroupé : " + contents.counts());

        // ---- Écran du coffre : tri, tout prendre, tout déposer
        server(s -> player(s).openMenu((MenuProvider) s.overworld().getBlockEntity(chestA)));
        waitFor("coffre ouvert", mc -> mc.gui.screen() instanceof ContainerScreen, 100);
        waitTicks(2);
        screenshot("eei_3_coffre_boutons");
        clickButton("⇅");
        waitTicks(5);
        server(s -> {
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
        screenshot("eei_4_coffre_trie");

        clickButton("↓");
        waitTicks(5);
        server(s -> check(container(s, chestA).isEmpty(), "tout prendre : le coffre doit être vide"));

        clickButton("↑");
        waitTicks(5);
        server(s -> {
            Inventory inv = player(s).getInventory();
            check(inv.getItem(13).is(Items.DIRT), "tout déposer : la case verrouillée reste");
            check(inv.getItem(0).is(Items.COBBLESTONE), "tout déposer : la hotbar reste");
            for (int i = 9; i < 36; i++) {
                check(i == 13 || inv.getItem(i).isEmpty(), "tout déposer : case " + i + " pas vidée");
            }
        });

        // ---- Raccourcis souris (coffre toujours ouvert)
        server(s -> {
            container(s, chestA).clearContent();
            Inventory inv = player(s).getInventory();
            inv.setItem(9, new ItemStack(Items.COBBLESTONE, 10));
            inv.setItem(10, new ItemStack(Items.COBBLESTONE, 10));
            inv.setItem(11, new ItemStack(Items.DIRT, 5));
            inv.setItem(12, new ItemStack(Items.DIRT, 5));
            player(s).containerMenu.broadcastChanges();
        });
        waitTicks(5);
        // Menu d'un coffre simple : cases 0-26 pour le coffre, puis 27-53 pour les cases 9-35 de l'inventaire.
        int cobbleA = 27;
        int cobbleB = 28;
        int dirtA = 29;

        client(mc -> {
            double[] p = slotCenter(mc, cobbleA);
            mc.gui.screen().mouseScrolled(p[0], p[1], 0, -1);
        });
        waitTicks(5);
        server(s -> {
            check(count(container(s, chestA), Items.COBBLESTONE) == 1, "molette : 1 cobblestone dans le coffre");
            check(player(s).getInventory().getItem(9).getCount() == 9, "molette : 9 cobblestones restent");
        });

        clickSlot(dirtA, InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT);
        waitTicks(5);
        server(s -> check(count(container(s, chestA), Items.DIRT) == 10, "Ctrl+Maj+clic : toute la terre part ; coffre = " + count(container(s, chestA), Items.DIRT)));

        // Maj + glisser depuis une case pleine : le clic vanilla envoie la première pile, le glisser du mod la seconde.
        client(mc -> {
            Screen screen = mc.gui.screen();
            double[] a = slotCenter(mc, cobbleA);
            double[] b = slotCenter(mc, cobbleB);
            MouseButtonInfo button = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOD_SHIFT);
            screen.mouseClicked(new MouseButtonEvent(a[0], a[1], button), false);
            screen.mouseDragged(new MouseButtonEvent((a[0] + b[0]) / 2, b[1], button), (b[0] - a[0]) / 2, 0);
            screen.mouseDragged(new MouseButtonEvent(b[0], b[1], button), (b[0] - a[0]) / 2, 0);
            screen.mouseReleased(new MouseButtonEvent(b[0], b[1], button));
        });
        waitTicks(5);
        server(s -> check(count(container(s, chestA), Items.COBBLESTONE) == 20, "Maj+glisser : les deux piles partent ; coffre = " + count(container(s, chestA), Items.COBBLESTONE)));
        screenshot("eei_5_raccourcis");

        client(mc -> mc.player.closeContainer());
        waitTicks(5);

        // ---- Remplissage auto de la hotbar
        server(s -> {
            Inventory inv = player(s).getInventory();
            inv.setSelectedSlot(1);
            inv.setItem(1, new ItemStack(Items.TORCH, 1));
            inv.setItem(20, new ItemStack(Items.TORCH, 30));
        });
        waitTicks(3);
        server(s -> player(s).getInventory().getItem(1).shrink(1));
        waitTicks(3);
        server(s -> {
            Inventory inv = player(s).getInventory();
            check(inv.getItem(1).is(Items.TORCH) && inv.getItem(1).getCount() == 30, "remplissage : 30 torches en main ; case 1 = " + inv.getItem(1));
            check(inv.getItem(20).isEmpty(), "remplissage : la réserve doit être prise");
        });
    }

    // ---------------------------------------------------------------- entrées

    /** Clic gauche au centre d'une case du menu ouvert, avec ces modificateurs. */
    private static void clickSlot(int menuSlot, int modifiers) {
        client(mc -> {
            double[] p = slotCenter(mc, menuSlot);
            MouseButtonEvent event = new MouseButtonEvent(p[0], p[1], new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, modifiers));
            mc.gui.screen().mouseClicked(event, false);
            mc.gui.screen().mouseReleased(event);
        });
    }

    /** Clic sur le premier bouton de l'écran dont le texte est exactement celui-ci. */
    private static void clickButton(String text) {
        client(mc -> {
            AbstractWidget button = mc.gui.screen().children().stream()
                    .filter(c -> c instanceof AbstractWidget w && w.getMessage().getString().equals(text))
                    .map(AbstractWidget.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("bouton « " + text + " » absent"));
            MouseButtonEvent event = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            mc.gui.screen().mouseClicked(event, false);
            mc.gui.screen().mouseReleased(event);
        });
    }

    private static double[] slotCenter(Minecraft mc, int menuSlot) {
        EeiScreenAccess screen = (EeiScreenAccess) mc.gui.screen();
        Slot slot = screen.eei$menu().slots.get(menuSlot);
        return new double[]{screen.eei$leftPos() + slot.x + 8, screen.eei$topPos() + slot.y + 8};
    }

    private static void screenshot(String name) {
        client(mc -> Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {}));
        waitTicks(2);
    }

    // ---------------------------------------------------------------- fils d'exécution

    private static void client(Consumer<Minecraft> action) {
        Minecraft mc = Minecraft.getInstance();
        mc.submit(() -> action.accept(mc)).join();
    }

    private static <T> T onClient(Function<Minecraft, T> action) {
        Minecraft mc = Minecraft.getInstance();
        return mc.submit(() -> action.apply(mc)).join();
    }

    private static void server(Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        server.submit(() -> action.accept(server)).join();
    }

    private static <T> T onServer(Function<MinecraftServer, T> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        return server.submit(() -> action.apply(server)).join();
    }

    private static void waitTicks(int ticks) {
        long target = TICKS.get() + ticks;
        while (TICKS.get() < target) {
            sleep();
        }
    }

    private static void waitFor(String what, Predicate<Minecraft> condition, int maxTicks) {
        long deadline = TICKS.get() + maxTicks;
        while (!onClient(condition::test)) {
            if (TICKS.get() > deadline) {
                throw new AssertionError("délai dépassé : " + what);
            }
            sleep();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- monde

    private static void command(String command) {
        server(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), command));
    }

    private static void setBlock(BlockPos pos, String block) {
        command("setblock %d %d %d minecraft:%s".formatted(pos.getX(), pos.getY(), pos.getZ(), block));
    }

    private static void putInBlock(BlockPos pos, int slot, String item) {
        command("item replace block %d %d %d container.%d with minecraft:%s".formatted(pos.getX(), pos.getY(), pos.getZ(), slot, item));
    }

    private static void putInPlayer(String slot, String item) {
        command("item replace entity @a " + slot + " with minecraft:" + item);
    }

    private static void deleteWorld(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new IllegalStateException("ancien monde de test non supprimé", e);
        }
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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

package fr.every.everyeasyinventory.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import fr.every.everyeasyinventory.Constants;
import fr.every.everyeasyinventory.client.ClientState;
import fr.every.everyeasyinventory.client.EeiScreenAccess;
import fr.every.everyeasyinventory.client.EeiSettingsScreen;
import fr.every.everyeasyinventory.client.Keys;
import fr.every.everyeasyinventory.config.EeiConfig;
import fr.every.everyeasyinventory.config.EeiSetting;
import fr.every.everyeasyinventory.logic.ContainerFinder;
import fr.every.everyeasyinventory.logic.PlayerState;
import fr.every.everyeasyinventory.logic.ServerActions;
import fr.every.everyeasyinventory.platform.Services;
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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Même scénario que le gametest client Fabric, joué dans un vrai client NeoForge avec un monde solo :
 * rangement express, case verrouillée (Alt + clic), hologramme, tri, tout prendre / tout déposer, molette,
 * Ctrl+Maj+clic, Maj+glisser, remplissage de la hotbar, recherche d'item, renommage d'un conteneur,
 * écran de réglages, touche de tri réassignée, réglages forcés par le serveur avec /eei,
 * Remplir, listes de conteneurs, stockage d'un autre mod.
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

    public EeiSelfTest(IEventBus modBus) {
        TestStorage.register(modBus);
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

        // ---- Recherche d'item : un diamant dans l'inventaire, d'autres dans une shulker rangée dans un coffre proche
        BlockPos shulkerChest = feet.offset(0, 0, 3);
        BlockPos farDiamonds = feet.offset(0, 0, -12);
        setBlock(shulkerChest, "chest");
        setBlock(farDiamonds, "chest");
        putInBlock(shulkerChest, 0, "shulker_box[container=[{slot:0,item:{id:\"minecraft:diamond\",count:3}}]] 1");
        putInBlock(farDiamonds, 0, "diamond 5");
        putInPlayer("inventory.6", "diamond 1"); // case 15
        waitTicks(5);

        client(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
        waitFor("inventaire ouvert", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(2);
        clickSearchBox();
        typeText("Diam");
        pressKey(InputConstants.KEY_E); // ne doit pas fermer l'inventaire
        check(onClient(mc -> mc.gui.screen() instanceof InventoryScreen), "taper E dans la barre ne doit pas fermer l'inventaire");
        waitTicks(2);
        screenshot("eei_6_recherche_suggestions");
        pressKey(InputConstants.KEY_RETURN);
        waitTicks(2);
        check(onClient(mc -> ClientState.searchItem() == Items.DIAMOND), "Entrée doit choisir le diamant : " + onClient(mc -> ClientState.searchItem()));
        check(onClient(mc -> searchBoxValue(mc)).equals("Diamond"), "le nom choisi reste dans la barre");
        screenshot("eei_7_recherche_inventaire");

        client(mc -> mc.player.closeContainer());
        waitFor("résultats de recherche", mc -> !ClientState.searchResults().isEmpty(), 100);
        check(onClient(mc -> ClientState.searchResults()).equals(java.util.List.of(shulkerChest)),
                "seul le coffre à la shulker est trouvé : " + onClient(mc -> ClientState.searchResults()));
        client(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(shulkerChest)));
        waitTicks(5);
        screenshot("eei_8_recherche_contour");

        // Le diamant quitte la shulker : le coffre n'est plus entouré.
        server(s -> container(s, shulkerChest).clearContent());
        waitFor("coffre vidé retiré des résultats", mc -> ClientState.searchResults().isEmpty(), 100);

        // Vider la barre arrête la recherche.
        client(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
        waitFor("inventaire ouvert", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(2);
        check(onClient(mc -> searchBoxValue(mc)).equals("Diamond"), "la recherche suit le joueur d'un écran à l'autre");
        clickSearchBox();
        pressKey(InputConstants.KEY_END); // le clic a mis le curseur au début du texte
        for (int i = 0; i < "Diamond".length(); i++) {
            pressKey(InputConstants.KEY_BACKSPACE);
        }
        waitTicks(2);
        check(onClient(mc -> ClientState.searchItem() == null && ClientState.searchMembers().isEmpty()), "barre vidée : plus de recherche ; barre = \"" + onClient(EeiSelfTest::searchBoxValue) + "\", item = " + onClient(mc -> ClientState.searchItem()) + ", focus = " + onClient(mc -> searchBox(mc).isFocused()));
        client(mc -> mc.player.closeContainer());
        waitTicks(3);

        // ---- Renommer un conteneur : clic sur le titre, saisie, Entrée
        openLookedAt(chestA);
        clickTitle();
        check(onClient(mc -> renameBox(mc).isVisible() && renameBox(mc).isFocused()), "un clic sur le titre ouvre le champ de renommage");
        check(onClient(mc -> mc.gui.screen().getTitle().getString()).isEmpty(), "l'ancien titre ne doit pas se superposer au champ");
        typeText("Minerais");
        screenshot("eei_9a_renommage_saisie");
        pressKey(InputConstants.KEY_RETURN);
        waitTicks(5);
        check(onClient(mc -> mc.gui.screen().getTitle().getString()).equals("Minerais"), "le titre affiche le nouveau nom");
        server(s -> {
            Component name = ((BaseContainerBlockEntity) s.overworld().getBlockEntity(chestA)).getCustomName();
            check(name != null && name.getString().equals("Minerais"), "le serveur pose le nom vanilla : " + name);
        });
        screenshot("eei_9_renommage");

        // Échap annule sans fermer le coffre.
        clickTitle();
        typeText("X");
        pressKey(InputConstants.KEY_ESCAPE);
        waitTicks(3);
        check(onClient(mc -> mc.gui.screen() instanceof ContainerScreen), "Échap pendant le renommage ne ferme pas le coffre");
        check(onClient(mc -> mc.gui.screen().getTitle().getString()).equals("Minerais"), "Échap annule le renommage");

        // Nom au-dessus de l'hologramme, même quand le coffre est vide.
        client(mc -> mc.player.closeContainer());
        client(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(chestA)));
        waitFor("nom dans l'hologramme", mc -> ClientState.contents() != null && ClientState.contents().name().isPresent(), 100);
        check(onClient(mc -> ClientState.contents().name().get().getString()).equals("Minerais"), "nom de l'hologramme");
        waitTicks(3);
        screenshot("eei_10_hologramme_nom");
        server(s -> container(s, chestA).clearContent());
        waitFor("hologramme du coffre vide", mc -> ClientState.contents() != null && ClientState.contents().icons().isEmpty(), 100);
        check(onClient(mc -> ClientState.contents().name().isPresent()), "le nom reste affiché quand le coffre est vide");
        waitTicks(3);
        screenshot("eei_11_hologramme_nom_vide");

        // Champ vidé = nom par défaut.
        openLookedAt(chestA);
        clickTitle();
        pressKey(InputConstants.KEY_END);
        for (int i = 0; i < "Minerais".length(); i++) {
            pressKey(InputConstants.KEY_BACKSPACE);
        }
        pressKey(InputConstants.KEY_RETURN);
        waitTicks(5);
        server(s -> check(((BaseContainerBlockEntity) s.overworld().getBlockEntity(chestA)).getCustomName() == null, "champ vidé : nom par défaut"));
        check(onClient(mc -> mc.gui.screen().getTitle().getString()).equals("Chest"), "titre par défaut : " + onClient(mc -> mc.gui.screen().getTitle().getString()));
        client(mc -> mc.player.closeContainer());
        waitTicks(3);

        // ---- Réglages : le bouton ⚙ ouvre l'écran, Échap revient à l'inventaire
        client(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
        waitFor("inventaire ouvert", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(2);
        clickButton("⚙");
        waitFor("réglages ouverts", mc -> mc.gui.screen() instanceof EeiSettingsScreen, 100);
        screenshot("eei_12_reglages");
        pressKey(InputConstants.KEY_ESCAPE);
        waitFor("retour à l'inventaire", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(2);

        // ---- Touche de tri réassignée : R ne trie plus, G trie
        server(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(9, new ItemStack(Items.COBBLESTONE, 10));
            inv.setItem(12, new ItemStack(Items.COBBLESTONE, 10));
            player(s).containerMenu.broadcastChanges();
        });
        waitTicks(5);
        client(mc -> {
            Keys.SORT.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_G));
            KeyMapping.resetMapping();
        });
        pressKey(InputConstants.KEY_R);
        waitTicks(5);
        server(s -> {
            check(player(s).getInventory().getItem(9).getCount() == 10, "R ne doit plus trier une fois la touche changée");
        });
        pressKey(InputConstants.KEY_G);
        waitTicks(5);
        server(s -> {
            Inventory inv = player(s).getInventory();
            check(inv.getItem(9).getCount() == 20 && inv.getItem(12).isEmpty(), "la nouvelle touche doit trier : case 9 = " + inv.getItem(9));
        });
        client(mc -> {
            Keys.SORT.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_R));
            KeyMapping.resetMapping();
            mc.player.closeContainer();
        });
        waitTicks(3);

        // ---- Couleur et épaisseur du contour, contour désactivé
        server(s -> {
            container(s, chestA).setItem(0, new ItemStack(Items.DIAMOND, 2));
        });
        client(mc -> {
            EeiConfig.get().outlineColor = 0xFF3B30;
            EeiConfig.get().outlineThickness = 5;
            ClientState.settingsChanged();
            ClientState.setSearch("Diamond", Items.DIAMOND);
        });
        waitFor("!ClientState.searchResults().isEmpty()", mc -> !ClientState.searchResults().isEmpty(), 100);
        client(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(chestA)));
        waitTicks(5);
        screenshot("eei_13_contour_reglages");
        client(mc -> {
            EeiConfig.get().outlineEnabled = false;
            ClientState.settingsChanged();
        });
        check(onClient(mc -> ClientState.searchResults().isEmpty()), "contour désactivé : plus de résultats");
        waitTicks(25);
        server(s -> {
            check(PlayerState.of(player(s)).searchMembers.isEmpty(), "contour désactivé : le serveur ne cherche plus");
        });
        client(mc -> {
            EeiConfig.get().outlineEnabled = true;
            EeiConfig.get().outlineColor = 0xFFC21A;
            EeiConfig.get().outlineThickness = 3;
            ClientState.settingsChanged();
        });
        waitFor("!ClientState.searchResults().isEmpty()", mc -> !ClientState.searchResults().isEmpty(), 100);

        // ---- Réglages forcés par le serveur avec /eei
        command("eei force outlineColor #FF0000");
        waitFor("ClientState.isForced(EeiSetting.OUTLINE_COLOR)", mc -> ClientState.isForced(EeiSetting.OUTLINE_COLOR), 100);
        check(onClient(mc -> (Integer) ClientState.value(EeiSetting.OUTLINE_COLOR) == 0xFF0000), "couleur forcée");
        check(onClient(mc -> EeiConfig.get().outlineColor == 0xFFC21A), "un réglage forcé ne change pas celui du joueur");
        client(mc -> mc.gui.setScreen(new EeiSettingsScreen(null)));
        waitFor("réglages ouverts", mc -> mc.gui.screen() instanceof EeiSettingsScreen, 100);
        waitTicks(2);
        screenshot("eei_14_reglages_forces");
        check(onClient(mc -> mc.gui.screen().children().stream().anyMatch(c -> c instanceof EditBox box && !box.active)), "le champ de couleur forcé doit être grisé");
        client(mc -> mc.gui.setScreen(null));
        waitTicks(2);

        command("eei force searchEnabled false");
        waitFor("!ClientState.bool(EeiSetting.SEARCH_ENABLED)", mc -> !ClientState.bool(EeiSetting.SEARCH_ENABLED), 100);
        check(onClient(mc -> ClientState.searchItem() == null), "recherche forcée à non : la recherche en cours est oubliée");
        client(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
        waitFor("inventaire ouvert", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(2);
        check(onClient(mc -> mc.gui.screen().children().stream().noneMatch(EditBox.class::isInstance)), "recherche forcée à non : pas de barre");
        client(mc -> mc.gui.setScreen(null));
        waitTicks(2);
        waitTicks(25);
        server(s -> {
            check(PlayerState.of(player(s)).searchMembers.isEmpty(), "recherche forcée à non : le serveur ne cherche plus");
        });

        command("eei force hologramEnabled false");
        waitFor("!ClientState.bool(EeiSetting.HOLOGRAM_ENABLED)", mc -> !ClientState.bool(EeiSetting.HOLOGRAM_ENABLED), 100);
        waitTicks(10);
        check(onClient(mc -> ClientState.contents() == null), "hologramme forcé à non : plus de résumé");

        command("eei force depositRadius 1");
        waitFor("ClientState.isForced(EeiSetting.DEPOSIT_RADIUS)", mc -> ClientState.isForced(EeiSetting.DEPOSIT_RADIUS), 100);
        server(s -> {
            Inventory inv = player(s).getInventory();
            inv.setItem(15, new ItemStack(Items.DIAMOND, 1));
        });
        waitTicks(3);
        client(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
        waitFor("inventaire ouvert", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(2);
        clickButton("Send");
        waitTicks(5);
        server(s -> {
            check(player(s).getInventory().getItem(15).is(Items.DIAMOND), "rayon forcé à 1 : le coffre à 2 blocs est hors de portée");
        });

        command("eei free outlineColor");
        command("eei free searchEnabled");
        command("eei free hologramEnabled");
        command("eei free depositRadius");
        waitFor("!ClientState.isForced(EeiSetting.DEPOSIT_RADIUS) && !ClientState.isForced(EeiSetting.SEARCH_ENABLED)", mc -> !ClientState.isForced(EeiSetting.DEPOSIT_RADIUS) && !ClientState.isForced(EeiSetting.SEARCH_ENABLED), 100);
        waitTicks(25);
        clickButton("Send");
        waitTicks(5);
        server(s -> {
            check(player(s).getInventory().getItem(15).isEmpty(), "rayon libéré : le diamant part dans le coffre");
        });
        command("eei reload");
        client(mc -> {
            mc.player.closeContainer();
        });
        waitTicks(3);

        // ---- Remplir : complète les piles entamées, sans toucher aux cases vides ni verrouillées
        server(s -> {
            Inventory inv = player(s).getInventory();
            inv.clearContent();
            inv.setItem(1, new ItemStack(Items.COBBLESTONE, 5));
            inv.setItem(15, new ItemStack(Items.COBBLESTONE, 10));
            inv.setItem(13, new ItemStack(Items.DIRT, 5)); // verrouillée
            Container chest = container(s, chestA);
            chest.clearContent();
            chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
            chest.setItem(1, new ItemStack(Items.DIRT, 20));
            chest.setItem(2, new ItemStack(Items.APPLE, 3));
        });
        waitTicks(25);
        client(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
        waitFor("inventaire ouvert", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(2);
        screenshot("eei_15_boutons_icones");
        clickButton("Fill");
        waitTicks(5);
        server(s -> {
            Inventory inv = player(s).getInventory();
            check(inv.getItem(1).getCount() == 64, "Remplir : la pile de la hotbar est complétée : " + inv.getItem(1));
            check(inv.getItem(15).getCount() == 15, "Remplir : le reste va à la pile suivante : " + inv.getItem(15));
            check(inv.getItem(13).getCount() == 5, "Remplir : la case verrouillée ne bouge pas");
            check(count(inv, Items.APPLE) == 0, "Remplir : un item absent de l'inventaire n'est pas pris");
            check(count(container(s, chestA), Items.COBBLESTONE) == 0 && count(container(s, chestA), Items.DIRT) == 20, "Remplir : le coffre perd seulement ce qui est pris");
        });

        // ---- Listes du serveur : un distributeur n'est pas géré, sauf s'il est ajouté ; un tonneau peut être exclu
        BlockPos dropper = feet.offset(2, 0, 2);
        setBlock(dropper, "dropper");
        putInBlock(dropper, 0, "iron_ingot 1");
        server(s -> player(s).getInventory().setItem(20, new ItemStack(Items.IRON_INGOT, 4)));
        waitTicks(25);
        clickButton("Send");
        waitTicks(5);
        server(s -> check(count(container(s, dropper), Items.IRON_INGOT) == 1, "un distributeur n'est pas un stockage par défaut"));
        Path rules = Services.PLATFORM.getConfigDir().resolve("everyeasyinventorymod-server.json");
        Files.writeString(rules, "{\"extraContainers\": [\"minecraft:dropper\"], \"excludedContainers\": [\"minecraft:barrel\"]}");
        command("eei reload");
        waitTicks(25);
        check(onClient(mc -> ContainerFinder.isSupported(mc.level.getBlockEntity(dropper))), "liste : le distributeur est ajouté");
        check(onClient(mc -> !ContainerFinder.isSupported(mc.level.getBlockEntity(barrel))), "liste : le tonneau est exclu");
        clickButton("Send");
        waitTicks(5);
        server(s -> check(count(container(s, dropper), Items.IRON_INGOT) == 5, "liste : Envoyer remplit le distributeur ajouté"));
        Files.writeString(rules, "{}");
        command("eei reload");
        waitTicks(25);

        // ---- Bloc de stockage d'un autre mod (voir TestStorage) : reconnu sans aucune liste
        BlockPos modded = feet.offset(-2, 0, 2);
        command("setblock %d %d %d eei_selftest:test_storage".formatted(modded.getX(), modded.getY(), modded.getZ()));
        waitTicks(3);
        check(onClient(mc -> ContainerFinder.isSupported(mc.level.getBlockEntity(modded))), "stockage d'un autre mod reconnu");
        server(s -> {
            container(s, modded).setItem(0, new ItemStack(Items.GOLD_INGOT, 1));
            player(s).getInventory().setItem(21, new ItemStack(Items.GOLD_INGOT, 5));
        });
        clickButton("Send");
        waitTicks(5);
        server(s -> {
            check(count(container(s, modded), Items.GOLD_INGOT) == 6, "autre mod : Envoyer y dépose");
            player(s).getInventory().setItem(22, new ItemStack(Items.GOLD_INGOT, 1));
        });
        waitTicks(25);
        clickButton("Fill");
        waitTicks(5);
        server(s -> {
            check(player(s).getInventory().getItem(22).getCount() == 7, "autre mod : Remplir y prend");
            container(s, modded).setItem(0, new ItemStack(Items.EMERALD, 2));
        });
        client(mc -> {
            mc.player.closeContainer();
            ClientState.setSearch("Emerald", Items.EMERALD);
        });
        waitFor("recherche dans le stockage d'un autre mod", mc -> ClientState.searchResults().contains(modded), 100);
        client(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(modded)));
        waitFor("hologramme du stockage d'un autre mod", mc -> ClientState.contents() != null && ClientState.contents().pos().equals(modded)
                && !ClientState.contents().icons().isEmpty() && ClientState.contents().icons().getFirst().is(Items.EMERALD), 100);
        waitTicks(3);
        screenshot("eei_16_autre_mod");
        client(mc -> ClientState.setSearch("", null));

        // ---- Bloc d'un autre mod qui n'expose qu'une capacité de NeoForge
        BlockPos capability = feet.offset(-2, 0, -2);
        command("setblock %d %d %d eei_selftest:test_capability_storage".formatted(capability.getX(), capability.getY(), capability.getZ()));
        waitTicks(3);
        check(onClient(mc -> ContainerFinder.isSupported(mc.level.getBlockEntity(capability))), "capacité : stockage reconnu");
        server(s -> {
            ContainerFinder.find(s.overworld(), capability).storage().fillEmpty(new ItemStack(Items.LAPIS_LAZULI, 1));
            player(s).getInventory().setItem(23, new ItemStack(Items.LAPIS_LAZULI, 5));
        });
        client(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
        waitFor("inventaire ouvert", mc -> mc.gui.screen() instanceof InventoryScreen, 100);
        waitTicks(25);
        clickButton("Send");
        waitTicks(5);
        server(s -> {
            check(ContainerFinder.find(s.overworld(), capability).storage().get(0).getCount() == 6, "capacité : Envoyer y dépose");
            player(s).getInventory().setItem(24, new ItemStack(Items.LAPIS_LAZULI, 1));
        });
        waitTicks(25);
        clickButton("Fill");
        waitTicks(5);
        server(s -> check(player(s).getInventory().getItem(24).getCount() == 7, "capacité : Remplir y prend"));
        client(mc -> {
            mc.player.closeContainer();
            ClientState.setSearch("Lapis Lazuli", Items.LAPIS_LAZULI);
        });
        server(s -> ContainerFinder.find(s.overworld(), capability).storage().fillEmpty(new ItemStack(Items.LAPIS_LAZULI, 3)));
        waitFor("recherche par capacité", mc -> ClientState.searchResults().contains(capability), 100);
        client(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(capability)));
        waitFor("hologramme par capacité", mc -> ClientState.contents() != null && ClientState.contents().pos().equals(capability), 100);
        client(mc -> ClientState.setSearch("", null));

        // ---- Écrans : un menu fait de simples cases ressemble à un coffre, pas un menu à cases spécialisées
        check(ServerActions.isChestLikeMenu(testMenu(false)), "menu d'un autre mod à cases simples : comme un coffre");
        check(!ServerActions.isChestLikeMenu(testMenu(true)), "menu à cases spécialisées : pas un coffre");
    }

    private static AbstractContainerMenu testMenu(boolean specialSlot) {
        AbstractContainerMenu menu = new AbstractContainerMenu(null, 1) {
            {
                SimpleContainer storage = new SimpleContainer(18);
                for (int i = 0; i < 18; i++) {
                    addSlot(specialSlot && i == 0 ? new Slot(storage, i, 0, 0) { } : new Slot(storage, i, 0, 0));
                }
            }

            @Override
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return true;
            }
        };
        return menu;
    }

    /** Ouvre le conteneur comme un clic droit : le joueur le regarde, puis le serveur ouvre son menu. */
    private static void openLookedAt(BlockPos pos) {
        client(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos)));
        waitTicks(3);
        server(s -> player(s).openMenu((MenuProvider) s.overworld().getBlockEntity(pos)));
        waitFor("conteneur ouvert", mc -> mc.gui.screen() instanceof ContainerScreen, 100);
        waitTicks(2);
    }

    private static EditBox renameBox(Minecraft mc) {
        return mc.gui.screen().children().stream()
                .filter(c -> c instanceof EditBox box && box.getMessage().getString().equals("Rename container"))
                .map(EditBox.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("champ de renommage absent"));
    }

    private static void clickTitle() {
        client(mc -> {
            EeiScreenAccess screen = (EeiScreenAccess) mc.gui.screen();
            MouseButtonEvent event = new MouseButtonEvent(screen.eei$leftPos() + 12, screen.eei$topPos() + screen.eei$titleLabelY() + 4,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            mc.gui.screen().mouseClicked(event, false);
            mc.gui.screen().mouseReleased(event);
        });
    }

    // ---------------------------------------------------------------- entrées

    private static EditBox searchBox(Minecraft mc) {
        return mc.gui.screen().children().stream()
                .filter(c -> c instanceof EditBox box && box.getMessage().getString().equals("Item search"))
                .map(EditBox.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("barre de recherche absente"));
    }

    private static String searchBoxValue(Minecraft mc) {
        return searchBox(mc).getValue();
    }

    private static void clickSearchBox() {
        client(mc -> {
            EditBox box = searchBox(mc);
            MouseButtonEvent event = new MouseButtonEvent(box.getX() + 4, box.getY() + box.getHeight() / 2.0,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            mc.gui.screen().mouseClicked(event, false);
            mc.gui.screen().mouseReleased(event);
            check(box.isFocused(), "un clic doit donner le focus à la barre");
        });
    }

    private static void typeText(String text) {
        client(mc -> text.codePoints().forEach(c -> mc.gui.screen().charTyped(new CharacterEvent(c))));
    }

    /** Touche avec son code de touche (que lit {@code EditBox}) en plus de son code physique. */
    private static void pressKey(int key) {
        int keycode = switch (key) {
            case InputConstants.KEY_BACKSPACE -> 8;
            case InputConstants.KEY_RETURN -> 13;
            case InputConstants.KEY_END -> 1073741901;
            case InputConstants.KEY_E -> 'e';
            case InputConstants.KEY_ESCAPE -> 27;
            default -> 0;
        };
        client(mc -> mc.gui.screen().keyPressed(new KeyEvent(key, keycode, 0)));
    }

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

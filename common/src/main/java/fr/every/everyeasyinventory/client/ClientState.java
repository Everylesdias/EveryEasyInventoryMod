package fr.every.everyeasyinventory.client;

import fr.every.everyeasyinventory.config.EeiConfig;
import fr.every.everyeasyinventory.config.EeiSetting;
import fr.every.everyeasyinventory.logic.ContainerFinder;
import fr.every.everyeasyinventory.logic.ItemSearch;
import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * État côté client : présence du mod sur le serveur, conteneur visé, dernier résumé reçu pour l'hologramme, recherche d'item.
 */
public final class ClientState {

    /** Conteneur visé (position clé, la même pour les deux moitiés d'un coffre double). */
    static @Nullable BlockPos target;
    /** Une des positions réelles du conteneur visé, pour retrouver sa forme. */
    static @Nullable BlockPos targetBlock;
    static Payloads.@Nullable PeekContents contents;
    private static boolean lockedSlotsSent;

    /** Recherche d'item : texte de la barre (gardé d'un écran à l'autre), item choisi, son groupe et les conteneurs trouvés. */
    private static String searchText = "";
    private static @Nullable Item searchItem;
    private static Set<Item> searchMembers = Set.of();
    private static List<BlockPos> searchResults = List.of();
    /** Dernier item envoyé au serveur pour la recherche dans le monde. */
    private static @Nullable Item searchSent;

    /** Réglages forcés par le serveur connecté. */
    private static final Map<EeiSetting, Object> FORCED = new EnumMap<>(EeiSetting.class);

    private ClientState() {
    }

    /** Le serveur connecté a-t-il le mod ? (toujours vrai en solo) */
    public static boolean serverHasMod() {
        return Minecraft.getInstance().getConnection() != null && Services.PLATFORM.canSendToServer(Payloads.Sort.TYPE);
    }

    /** Dernier résumé reçu pour le conteneur visé, ou {@code null}. */
    public static Payloads.@Nullable PeekContents contents() {
        return contents != null && contents.pos().equals(target) ? contents : null;
    }

    public static void onPeekContents(Payloads.PeekContents payload) {
        contents = payload;
    }

    public static boolean isLocked(int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot < 64 && (EeiConfig.get().lockedSlots & (1L << inventorySlot)) != 0;
    }

    public static void toggleLock(int inventorySlot) {
        EeiConfig config = EeiConfig.get();
        config.lockedSlots ^= 1L << inventorySlot;
        config.save();
        if (serverHasMod()) {
            Services.PLATFORM.sendToServer(new Payloads.LockedSlots(config.lockedSlots));
        }
    }

    // ---------------------------------------------------------------- réglages

    /** Valeur qui s'applique : celle forcée par le serveur, sinon celle du joueur. */
    public static Object value(EeiSetting setting) {
        Object forced = FORCED.get(setting);
        return forced != null ? forced : setting.read(EeiConfig.get());
    }

    public static boolean bool(EeiSetting setting) {
        return (Boolean) value(setting);
    }

    public static boolean isForced(EeiSetting setting) {
        return FORCED.containsKey(setting);
    }

    /** Le joueur vient de changer un réglage. */
    public static void settingsChanged() {
        EeiConfig.get().save();
        if (serverHasMod()) {
            sendSettings();
        }
        applySearchSettings();
    }

    private static void sendSettings() {
        Map<String, String> values = new HashMap<>();
        for (EeiSetting setting : EeiSetting.values()) {
            values.put(setting.key, setting.format(setting.read(EeiConfig.get())));
        }
        Services.PLATFORM.sendToServer(new Payloads.ClientSettings(values));
    }

    public static void onForcedSettings(Payloads.ForcedSettings payload) {
        FORCED.clear();
        payload.values().forEach((key, text) -> {
            EeiSetting setting = EeiSetting.byKey(key);
            Object value = setting == null ? null : setting.parse(text);
            if (value != null) {
                FORCED.put(setting, value);
            }
        });
        applySearchSettings();
        if (Minecraft.getInstance().gui.screen() instanceof EeiSettingsScreen screen) {
            screen.refresh();
        }
    }

    /** Recherche désactivée : on l'oublie. Sinon, le serveur ne cherche dans le monde que si le contour est affiché. */
    private static void applySearchSettings() {
        if (!bool(EeiSetting.SEARCH_ENABLED)) {
            setSearch("", null);
        }
        syncSearch();
    }

    private static void syncSearch() {
        Item wanted = bool(EeiSetting.OUTLINE_ENABLED) ? searchItem : null;
        if (wanted == null) {
            searchResults = List.of();
        }
        if (wanted != searchSent && serverHasMod()) {
            searchSent = wanted;
            Services.PLATFORM.sendToServer(new Payloads.Search(Optional.ofNullable(wanted).map(Item::builtInRegistryHolder)));
        }
    }

    // ---------------------------------------------------------------- recherche d'item

    public static String searchText() {
        return searchText;
    }

    public static @Nullable Item searchItem() {
        return searchItem;
    }

    /** Groupe de l'item cherché (vide sans recherche). */
    public static Set<Item> searchMembers() {
        return searchMembers;
    }

    /** Conteneurs proches qui ont l'item cherché (positions clés), selon le serveur. */
    public static List<BlockPos> searchResults() {
        return searchResults;
    }

    /** Nouveau texte de la barre ; {@code item} est l'item choisi, ou {@code null} tant qu'aucun n'est choisi. */
    public static void setSearch(String text, @Nullable Item item) {
        searchText = text;
        if (item == searchItem) {
            return;
        }
        searchItem = item;
        searchMembers = item == null ? Set.of() : ItemSearch.members(item);
        searchResults = List.of();
        syncSearch();
    }

    public static void onSearchResults(Payloads.SearchResults payload) {
        searchResults = searchItem == null ? List.of() : payload.containers();
    }

    /** Appelé à la fin de chaque tick client. */
    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            // Recherche oubliée en quittant le monde ou le serveur.
            searchText = "";
            searchItem = null;
            searchMembers = Set.of();
            searchResults = List.of();
            searchSent = null;
            FORCED.clear();
        }
        if (minecraft.player == null || minecraft.level == null || !serverHasMod()) {
            lockedSlotsSent = false;
            FORCED.clear();
            target = null;
            targetBlock = null;
            contents = null;
            return;
        }
        if (!lockedSlotsSent) {
            Services.PLATFORM.sendToServer(new Payloads.LockedSlots(EeiConfig.get().lockedSlots));
            sendSettings();
            lockedSlotsSent = true;
        }
        updateTarget(minecraft);
    }

    private static void updateTarget(Minecraft minecraft) {
        BlockPos newTarget = null;
        BlockPos newBlock = null;
        if (bool(EeiSetting.HOLOGRAM_ENABLED) && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && minecraft.player.getEyePosition().distanceTo(hit.getLocation()) <= (Double) value(EeiSetting.HOLOGRAM_DISTANCE)) {
            BlockPos pos = hit.getBlockPos();
            if (ContainerFinder.isSupported(minecraft.level.getBlockEntity(pos))) {
                BlockState state = minecraft.level.getBlockState(pos);
                newTarget = ContainerFinder.key(state, pos);
                newBlock = pos;
            }
        }
        if (newTarget == null) {
            if (target != null) {
                Services.PLATFORM.sendToServer(new Payloads.Peek(Optional.empty()));
            }
        } else if (!newTarget.equals(target)) {
            Services.PLATFORM.sendToServer(new Payloads.Peek(Optional.of(newBlock)));
        }
        target = newTarget;
        targetBlock = newBlock;
        if (contents != null && !contents.pos().equals(target)) {
            contents = null;
        }
    }
}

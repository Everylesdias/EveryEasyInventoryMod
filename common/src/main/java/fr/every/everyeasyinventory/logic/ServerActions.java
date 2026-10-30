package fr.every.everyeasyinventory.logic;

import fr.every.everyeasyinventory.config.EeiSetting;
import fr.every.everyeasyinventory.mixin.BaseContainerBlockEntityAccessor;
import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tout ce que le serveur fait à la demande du client ou à chaque tick d'un joueur.
 */
public final class ServerActions {

    /** Cases 9 à 35 de l'inventaire du joueur : l'inventaire principal, sans la hotbar. */
    private static final int MAIN_START = Inventory.getSelectionSize();
    private static final int MAIN_END = 36;
    private static final int MAX_HOLOGRAM_ENTRIES = 54;
    private static final double SEARCH_RADIUS = 10.0;
    private static final int MAX_SEARCH_RESULTS = 64;
    /** Comme l'enclume. */
    public static final int MAX_NAME_LENGTH = 50;

    private ServerActions() {
    }

    /** Menus dont la partie conteneur peut être triée. */
    public static boolean isSortableMenu(AbstractContainerMenu menu) {
        return menu instanceof DispenserMenu || menu instanceof HopperMenu || isChestLikeMenu(menu);
    }

    /**
     * Écran qui ressemble à un coffre : ceux du jeu, ou un menu d'un autre mod dont toutes les cases hors joueur sont du
     * même type, au moins 9. Les machines et les établis mélangent plusieurs types de cases (résultat, carburant...).
     */
    public static boolean isChestLikeMenu(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
            return true;
        }
        if (menu instanceof DispenserMenu || menu instanceof HopperMenu || menu instanceof CrafterMenu) {
            return false;
        }
        Class<?> slotType = null;
        int count = 0;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            if (slotType != null && slot.getClass() != slotType) {
                return false;
            }
            slotType = slot.getClass();
            count++;
        }
        return count >= 9;
    }

    // ---------------------------------------------------------------- tri

    public static void sort(ServerPlayer player, int containerId, boolean playerSide) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        PlayerState state = PlayerState.of(player);
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            boolean isPlayerSlot = slot.container == inventory;
            if (playerSide) {
                int index = slot.getContainerSlot();
                if (isPlayerSlot && index >= MAIN_START && index < MAIN_END && !state.isLocked(index)) {
                    slots.add(slot);
                }
            } else if (!isPlayerSlot && isSortableMenu(menu)) {
                slots.add(slot);
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        int limit = slots.getFirst().getMaxStackSize();
        List<ItemStack> sorted = ItemSorting.mergeAndSort(slots.stream().map(Slot::getItem).toList(), limit);
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).set(i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
        }
        menu.broadcastChanges();
    }

    // ---------------------------------------------------------------- rangement express

    public static void areaDeposit(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerState state = PlayerState.of(player);
        long now = level.getGameTime();
        if (now - state.lastDepositTick < 20) {
            return;
        }
        state.lastDepositTick = now;

        List<ContainerFinder.Target> targets = reachableContainers(player, level);
        List<Set<Item>> contents = targets.stream().map(target -> itemsIn(target.storage())).toList();

        Inventory inventory = player.getInventory();
        int first = ServerSettings.bool(player, EeiSetting.DEPOSIT_HOTBAR) ? 0 : MAIN_START;
        int moved = 0;
        Set<Integer> touched = new HashSet<>();
        for (int i = first; i < MAIN_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || state.isLocked(i)) {
                continue;
            }
            int before = stack.getCount();
            // 1. Compléter les piles identiques déjà présentes, du conteneur le plus proche au plus loin.
            for (int t = 0; t < targets.size() && !stack.isEmpty(); t++) {
                if (contents.get(t).contains(stack.getItem()) && targets.get(t).storage().fillExisting(stack)) {
                    touched.add(t);
                }
            }
            // 2. Puis les cases vides des conteneurs qui contiennent déjà cet item.
            for (int t = 0; t < targets.size() && !stack.isEmpty(); t++) {
                if (contents.get(t).contains(stack.getItem()) && targets.get(t).storage().fillEmpty(stack)) {
                    touched.add(t);
                }
            }
            if (stack.getCount() != before) {
                moved += before - stack.getCount();
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }

        if (moved == 0) {
            player.sendOverlayMessage(Component.translatable("message.everyeasyinventorymod.deposit.none"));
            return;
        }
        for (int t : touched) {
            targets.get(t).storage().setChanged();
            Vec3 center = targets.get(t).center();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.6, center.z, 6, 0.3, 0.2, 0.3, 0.0);
        }
        player.inventoryMenu.broadcastChanges();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
        player.sendOverlayMessage(Component.translatable("message.everyeasyinventorymod.deposit", moved, touched.size()));
    }

    /** Conteneurs à portée d'Envoyer et de Remplir : un cube centré sur le joueur. */
    private static List<ContainerFinder.Target> reachableContainers(ServerPlayer player, ServerLevel level) {
        int radius = ServerSettings.intValue(player, EeiSetting.DEPOSIT_RADIUS);
        BlockPos feet = player.blockPosition();
        return containersAround(player, level, radius,
                pos -> Math.abs(pos.getX() - feet.getX()) <= radius && Math.abs(pos.getY() - feet.getY()) <= radius
                        && Math.abs(pos.getZ() - feet.getZ()) <= radius);
    }

    /**
     * Conteneurs accessibles au joueur, du plus proche au plus loin. On ne parcourt que les block entities des tronçons
     * déjà chargés à {@code radius} blocs, jamais chaque position, et on ne charge aucun tronçon.
     */
    private static List<ContainerFinder.Target> containersAround(ServerPlayer player, ServerLevel level, double radius, Predicate<BlockPos> inside) {
        Vec3 eyes = player.getEyePosition();
        int minChunkX = SectionPos.blockToSectionCoord(eyes.x - radius - 1);
        int maxChunkX = SectionPos.blockToSectionCoord(eyes.x + radius + 1);
        int minChunkZ = SectionPos.blockToSectionCoord(eyes.z - radius - 1);
        int maxChunkZ = SectionPos.blockToSectionCoord(eyes.z + radius + 1);
        Set<BlockPos> seen = new HashSet<>();
        List<ContainerFinder.Target> targets = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (!ContainerFinder.isSupported(blockEntity) || !inside.test(pos)
                            || !seen.add(ContainerFinder.key(blockEntity.getBlockState(), pos))) {
                        continue;
                    }
                    ContainerFinder.Target target = ContainerFinder.find(level, pos);
                    if (target != null && ContainerFinder.canAccess(player, level, pos)) {
                        targets.add(target);
                    }
                }
            }
        }
        targets.sort(Comparator.comparingDouble(target -> target.center().distanceToSqr(eyes)));
        return targets;
    }

    private static Set<Item> itemsIn(EeiStorage storage) {
        Set<Item> items = new HashSet<>();
        for (int i = 0; i < storage.size(); i++) {
            ItemStack stack = storage.get(i);
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        return items;
    }

    // ---------------------------------------------------------------- remplir

    /**
     * Complète les piles entamées de l'inventaire et de la hotbar depuis les conteneurs à portée d'Envoyer,
     * du plus proche au plus loin. Les cases vides et les cases verrouillées ne bougent pas.
     */
    public static void quickFill(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        PlayerState state = PlayerState.of(player);
        long now = level.getGameTime();
        if (now - state.lastDepositTick < 20) {
            return;
        }
        state.lastDepositTick = now;

        Inventory inventory = player.getInventory();
        List<ContainerFinder.Target> targets = null;
        int moved = 0;
        Set<ContainerFinder.Target> touched = new HashSet<>();
        for (int i = 0; i < MAIN_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || state.isLocked(i) || stack.getCount() >= stack.getMaxStackSize()) {
                continue;
            }
            if (targets == null) {
                targets = reachableContainers(player, level);
            }
            for (ContainerFinder.Target target : targets) {
                int taken = target.storage().takeInto(stack, inventory);
                if (taken > 0) {
                    moved += taken;
                    touched.add(target);
                }
                if (stack.getCount() >= stack.getMaxStackSize()) {
                    break;
                }
            }
        }

        if (moved == 0) {
            player.sendOverlayMessage(Component.translatable("message.everyeasyinventorymod.fill.none"));
            return;
        }
        for (ContainerFinder.Target target : touched) {
            target.storage().setChanged();
            Vec3 center = target.center();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.6, center.z, 6, 0.3, 0.2, 0.3, 0.0);
        }
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 0.9F);
        player.sendOverlayMessage(Component.translatable("message.everyeasyinventorymod.fill", moved, touched.size()));
    }

    // ---------------------------------------------------------------- hologramme

    public static void peek(ServerPlayer player, Optional<BlockPos> pos) {
        PlayerState state = PlayerState.of(player);
        state.peekPos = !ServerSettings.bool(player, EeiSetting.HOLOGRAM_ENABLED) ? null
                : pos.filter(p -> isPeekable(player, p, ServerSettings.doubleValue(player, EeiSetting.HOLOGRAM_DISTANCE) + 1.5)).orElse(null);
        state.peekHash = 0;
        if (state.peekPos != null) {
            sendPeek(player, state, true);
        }
    }

    private static boolean isPeekable(ServerPlayer player, BlockPos pos, double maxDistance) {
        ServerLevel level = (ServerLevel) player.level();
        return level.isLoaded(pos)
                && ContainerFinder.isSupported(level.getBlockEntity(pos))
                && Vec3.atCenterOf(pos).distanceTo(player.getEyePosition()) <= maxDistance
                && ContainerFinder.canAccess(player, level, pos);
    }

    private static void sendPeek(ServerPlayer player, PlayerState state, boolean force) {
        ContainerFinder.Target target = ContainerFinder.find(player.level(), state.peekPos);
        if (target == null) {
            state.peekPos = null;
            return;
        }
        Map<Item, ItemStack> icons = new LinkedHashMap<>();
        Map<Item, Integer> counts = new LinkedHashMap<>();
        EeiStorage storage = target.storage();
        for (int i = 0; i < storage.size(); i++) {
            ItemStack stack = storage.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item group = groupOf(stack.getItem());
            if (!icons.containsKey(group) && icons.size() >= MAX_HOLOGRAM_ENTRIES) {
                continue;
            }
            icons.computeIfAbsent(group, g -> iconFor(stack, g));
            counts.merge(group, stack.getCount(), Integer::sum);
        }
        List<ItemStack> iconList = new ArrayList<>(icons.values());
        List<Integer> countList = new ArrayList<>(counts.values());
        Optional<Component> name = Optional.ofNullable(ContainerFinder.customName(player.level(), state.peekPos));
        int hash = Objects.hash(target.key(), name, icons.keySet(), countList);
        if (force || hash != state.peekHash) {
            state.peekHash = hash;
            Services.PLATFORM.sendToPlayer(player, new Payloads.PeekContents(target.key(), name, iconList, countList));
        }
    }

    /**
     * Item sous lequel regrouper un item dans l'hologramme : le cuivre oxydé ou ciré rejoint sa forme de base.
     */
    public static Item groupOf(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().getOrDefault(block, block);
            Item base = WeatheringCopper.getFirst(unwaxed).asItem();
            if (base != Items.AIR) {
                return base;
            }
        }
        return item;
    }

    private static ItemStack iconFor(ItemStack first, Item group) {
        if (first.getItem() != group) {
            return new ItemStack(group);
        }
        ItemStack icon = first.copyWithCount(1);
        icon.remove(DataComponents.DAMAGE);
        return icon;
    }

    // ---------------------------------------------------------------- renommage

    /**
     * Pose le nom vanilla du conteneur ouvert (les deux moitiés d'un coffre double), comme une enclume.
     * Refusé si le menu ouvert n'est pas celui de ce conteneur ou si le joueur n'y a pas accès.
     */
    public static void rename(ServerPlayer player, int containerId, BlockPos pos, String text) {
        AbstractContainerMenu menu = player.containerMenu;
        ServerLevel level = (ServerLevel) player.level();
        if (menu.containerId != containerId || !menu.stillValid(player) || menu.slots.isEmpty() || !level.isLoaded(pos)
                || !(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity blockEntity)
                || !ContainerFinder.isSupported(blockEntity) || !ContainerFinder.canAccess(player, level, pos)) {
            return;
        }
        Container shown = menu.slots.getFirst().container;
        if (shown != blockEntity && !(shown instanceof CompoundContainer compound && compound.contains(blockEntity))) {
            return;
        }
        String clean = StringUtil.truncateStringIfNecessary(StringUtil.filterText(text).trim(), MAX_NAME_LENGTH, false);
        Component name = clean.isEmpty() ? null : Component.literal(clean);
        BlockPos other = ContainerFinder.otherHalf(blockEntity.getBlockState(), pos);
        for (BlockPos half : other.equals(pos) ? List.of(pos) : List.of(pos, other)) {
            if (level.getBlockEntity(half) instanceof BaseContainerBlockEntity container) {
                ((BaseContainerBlockEntityAccessor) container).eei$setName(name);
                container.setChanged();
            }
        }
        PlayerState.of(player).peekHash = 0;
    }

    // ---------------------------------------------------------------- recherche d'item

    public static void search(ServerPlayer player, Optional<Item> item) {
        PlayerState state = PlayerState.of(player);
        state.searchMembers = item.filter(i -> i != Items.AIR && isSearchAllowed(player)).map(ItemSearch::members).orElse(Set.of());
        state.searchPos = null;
        state.searchHash = 0;
        if (!state.searchMembers.isEmpty()) {
            scanSearch(player, state, true);
        }
    }

    private static boolean isSearchAllowed(ServerPlayer player) {
        return ServerSettings.bool(player, EeiSetting.SEARCH_ENABLED) && ServerSettings.bool(player, EeiSetting.OUTLINE_ENABLED);
    }

    /** Conteneurs à {@link #SEARCH_RADIUS} blocs des yeux qui ont l'item cherché. */
    private static void scanSearch(ServerPlayer player, PlayerState state, boolean force) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eyes = player.getEyePosition();
        double maxSqr = SEARCH_RADIUS * SEARCH_RADIUS;
        List<BlockPos> keys = containersAround(player, level, SEARCH_RADIUS, pos -> Vec3.atCenterOf(pos).distanceToSqr(eyes) <= maxSqr).stream()
                .filter(target -> ItemSearch.contains(target.storage(), state.searchMembers))
                .limit(MAX_SEARCH_RESULTS).map(ContainerFinder.Target::key).toList();
        state.searchPos = player.blockPosition();
        int hash = keys.hashCode() * 31 + 1;
        if (force || hash != state.searchHash) {
            state.searchHash = hash;
            Services.PLATFORM.sendToPlayer(player, new Payloads.SearchResults(keys));
        }
    }

    // ---------------------------------------------------------------- tick

    public static void tick(ServerPlayer player) {
        PlayerState state = PlayerState.of(player);
        if (state.peekPos != null && player.tickCount % 5 == 0) {
            if (ServerSettings.bool(player, EeiSetting.HOLOGRAM_ENABLED)
                    && isPeekable(player, state.peekPos, ServerSettings.doubleValue(player, EeiSetting.HOLOGRAM_DISTANCE) + 2.5)) {
                sendPeek(player, state, false);
            } else {
                state.peekPos = null;
            }
        }
        // Recherche : toutes les 10 ticks si le joueur a bougé, sinon toutes les 20 pour voir les coffres qui changent.
        if (!state.searchMembers.isEmpty() && !isSearchAllowed(player)) {
            // Interdite entre-temps par le serveur (/eei) : plus aucun résultat.
            search(player, Optional.empty());
            Services.PLATFORM.sendToPlayer(player, new Payloads.SearchResults(List.of()));
        }
        if (!state.searchMembers.isEmpty() && player.tickCount % 10 == 0
                && (player.tickCount % 20 == 0 || !player.blockPosition().equals(state.searchPos))) {
            scanSearch(player, state, false);
        }
        if (ServerSettings.bool(player, EeiSetting.HOTBAR_REFILL)) {
            refillHotbar(player, state);
        }
    }

    /**
     * Quand la pile en main disparaît de l'inventaire (dernier bloc posé, nourriture finie, outil cassé),
     * la remplacer par une pile identique de l'inventaire principal.
     */
    private static void refillHotbar(ServerPlayer player, PlayerState state) {
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        ItemStack held = inventory.getItem(selected);
        boolean screenOpen = player.containerMenu != player.inventoryMenu;
        if (selected == state.lastSelectedSlot && held.isEmpty() && !state.lastHeld.isEmpty()
                && !screenOpen && !player.isSpectator()
                && count(player, state.lastHeld.getItem()) < state.lastHeldTotal) {
            int source = findRefill(inventory, state, state.lastHeld);
            if (source >= 0) {
                inventory.setItem(selected, inventory.getItem(source));
                inventory.setItem(source, ItemStack.EMPTY);
                held = inventory.getItem(selected);
            }
        }
        state.lastSelectedSlot = selected;
        state.lastHeld = held.copy();
        state.lastHeldTotal = held.isEmpty() ? 0 : count(player, held.getItem());
    }

    private static int findRefill(Inventory inventory, PlayerState state, ItemStack previous) {
        int sameItem = -1;
        for (int i = MAIN_START; i < MAIN_END; i++) {
            ItemStack candidate = inventory.getItem(i);
            if (candidate.isEmpty() || state.isLocked(i) || candidate.getItem() != previous.getItem()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(candidate, previous)) {
                return i;
            }
            if (sameItem < 0) {
                sameItem = i;
            }
        }
        return sameItem;
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.getItem() == item) {
            total += carried.getCount();
        }
        return total;
    }
}

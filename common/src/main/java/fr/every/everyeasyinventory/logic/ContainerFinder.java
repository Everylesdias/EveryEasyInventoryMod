package fr.every.everyeasyinventory.logic;

import fr.every.everyeasyinventory.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import java.util.Collection;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Trouve les conteneurs gérés par le mod : coffres (toutes variantes), tonneaux, boîtes de shulker posées, et les blocs
 * de stockage d'autres mods reconnus automatiquement. Le fichier de réglages du serveur peut en ajouter ou en exclure.
 * Un coffre double est vu comme un seul conteneur, identifié par la position de sa première moitié.
 */
public final class ContainerFinder {

    private ContainerFinder() {
    }

    /**
     * @param key    position qui identifie le conteneur (la même pour les deux moitiés d'un coffre double)
     * @param center centre du conteneur (au milieu des deux moitiés pour un coffre double)
     */
    public record Target(EeiStorage storage, BlockPos key, Vec3 center) {
    }

    /** Taille minimale pour qu'un inventaire inconnu soit vu comme un stockage : les machines ont peu de cases. */
    private static final int MIN_STORAGE_SIZE = 9;

    private static Set<String> extraBlocks = Set.of();
    private static Set<String> excludedBlocks = Set.of();

    /** Blocs ajoutés ou exclus, sous la forme {@code mod:bloc} ou {@code mod:*}. Fixés par le serveur. */
    public static void setRules(Collection<String> extra, Collection<String> excluded) {
        extraBlocks = Set.copyOf(extra);
        excludedBlocks = Set.copyOf(excluded);
    }

    public static boolean isSupported(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        if (matches(excludedBlocks, id)) {
            return false;
        }
        boolean extra = matches(extraBlocks, id);
        if (blockEntity instanceof Container container) {
            return extra || looksLikeStorage(container);
        }
        // Les blocs du jeu sans inventaire (panneaux, lits...) n'ont pas besoin de la recherche ci-dessous.
        if (id.getNamespace().equals("minecraft") || blockEntity.getLevel() == null) {
            return false;
        }
        EeiStorage storage = Services.PLATFORM.findStorage(blockEntity.getLevel(), blockEntity.getBlockPos());
        return storage != null && (extra || storage.looksLikeStorage());
    }

    private static boolean matches(Set<String> rules, Identifier id) {
        return rules.contains(id.toString()) || rules.contains(id.getNamespace() + ":*");
    }

    /**
     * Un stockage expose un inventaire d'au moins 9 cases, sans faces spécialisées comme les machines
     * ({@link WorldlyContainer}). Les shulkers sont l'exception ; distributeurs et crafters n'en sont pas.
     */
    private static boolean looksLikeStorage(Container container) {
        if (container instanceof ChestBlockEntity || container instanceof BarrelBlockEntity || container instanceof ShulkerBoxBlockEntity) {
            return true;
        }
        return !(container instanceof WorldlyContainer) && !(container instanceof DispenserBlockEntity)
                && !(container instanceof CrafterBlockEntity) && container.getContainerSize() >= MIN_STORAGE_SIZE;
    }

    /**
     * Position de l'autre moitié d'un coffre double, ou {@code pos} pour un conteneur simple. Utilisable côté client.
     */
    public static BlockPos otherHalf(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return pos.relative(ChestBlock.getConnectedDirection(state));
        }
        return pos;
    }

    /**
     * Position qui identifie le conteneur en {@code pos}, la même pour les deux moitiés d'un coffre double.
     */
    public static BlockPos key(BlockState state, BlockPos pos) {
        BlockPos other = otherHalf(state, pos);
        return other.asLong() < pos.asLong() ? other : pos;
    }

    public static @Nullable Target find(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!isSupported(blockEntity)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        EeiStorage storage;
        if (state.getBlock() instanceof ChestBlock chest && blockEntity instanceof Container) {
            Container container = ChestBlock.getContainer(chest, state, level, pos, true);
            storage = container != null ? new EeiStorage.OfContainer(container) : null;
        } else if (blockEntity instanceof Container container) {
            storage = new EeiStorage.OfContainer(container);
        } else {
            storage = Services.PLATFORM.findStorage(level, pos);
        }
        if (storage == null) {
            return null;
        }
        BlockPos other = otherHalf(state, pos);
        Vec3 center = Vec3.atCenterOf(pos).add(Vec3.atCenterOf(other)).scale(0.5);
        return new Target(storage, key(state, pos), center);
    }

    /** Nom donné au conteneur (enclume ou renommage), première moitié d'abord pour un coffre double, comme le jeu. */
    public static @Nullable Component customName(Level level, BlockPos pos) {
        BlockPos other = otherHalf(level.getBlockState(pos), pos);
        BlockPos first = key(level.getBlockState(pos), pos);
        BlockPos second = first.equals(pos) ? other : pos;
        for (BlockPos half : new BlockPos[]{first, second}) {
            if (level.getBlockEntity(half) instanceof BaseContainerBlockEntity base && base.getCustomName() != null) {
                return base.getCustomName();
            }
        }
        return null;
    }

    /**
     * Le joueur aurait-il le droit d'ouvrir ce conteneur ? Refusé si verrouillé, protégé (spawn, mod de claims),
     * si c'est un coffre à butin pas encore généré (on ne le déclenche pas à distance), ou si un autre mod refuse d'en
     * sortir les items (son propre verrou).
     */
    public static boolean canAccess(Player player, Level level, BlockPos pos) {
        if (!level.mayInteract(player, pos)) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof Container)) {
            EeiStorage storage = Services.PLATFORM.findStorage(level, pos);
            return storage != null && !storage.refusesExtraction();
        }
        BlockPos other = otherHalf(level.getBlockState(pos), pos);
        return canAccessHalf(player, level, pos) && (other.equals(pos) || canAccessHalf(player, level, other));
    }

    private static boolean canAccessHalf(Player player, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RandomizableContainerBlockEntity randomizable && randomizable.getLootTable() != null) {
            return false;
        }
        return !(blockEntity instanceof BaseContainerBlockEntity base) || base.canOpen(player);
    }
}

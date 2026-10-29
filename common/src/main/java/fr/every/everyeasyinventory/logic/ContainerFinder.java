package fr.every.everyeasyinventory.logic;

import net.minecraft.core.BlockPos;
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
 * Trouve les conteneurs gérés par le mod : coffres (toutes variantes), tonneaux, boîtes de shulker posées.
 * Un coffre double est vu comme un seul conteneur, identifié par la position de sa première moitié.
 */
public final class ContainerFinder {

    private ContainerFinder() {
    }

    /**
     * @param key    position qui identifie le conteneur (la même pour les deux moitiés d'un coffre double)
     * @param center centre du conteneur (au milieu des deux moitiés pour un coffre double)
     */
    public record Target(Container container, BlockPos key, Vec3 center) {
    }

    public static boolean isSupported(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity;
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
        Container container;
        if (state.getBlock() instanceof ChestBlock chest) {
            container = ChestBlock.getContainer(chest, state, level, pos, true);
        } else {
            container = (Container) blockEntity;
        }
        if (container == null) {
            return null;
        }
        BlockPos other = otherHalf(state, pos);
        Vec3 center = Vec3.atCenterOf(pos).add(Vec3.atCenterOf(other)).scale(0.5);
        return new Target(container, key(state, pos), center);
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
     * ou si c'est un coffre à butin pas encore généré : on ne le déclenche pas à distance.
     */
    public static boolean canAccess(Player player, Level level, BlockPos pos) {
        if (!level.mayInteract(player, pos)) {
            return false;
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

package fr.every.everyeasyinventory.client;

import fr.every.everyeasyinventory.config.EeiConfig;
import fr.every.everyeasyinventory.logic.ContainerFinder;
import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * État côté client : présence du mod sur le serveur, conteneur visé, dernier résumé reçu pour l'hologramme.
 */
public final class ClientState {

    /** Conteneur visé (position clé, la même pour les deux moitiés d'un coffre double). */
    static @Nullable BlockPos target;
    /** Une des positions réelles du conteneur visé, pour retrouver sa forme. */
    static @Nullable BlockPos targetBlock;
    static Payloads.@Nullable PeekContents contents;
    private static boolean lockedSlotsSent;

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

    /** Appelé à la fin de chaque tick client. */
    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || !serverHasMod()) {
            lockedSlotsSent = false;
            target = null;
            targetBlock = null;
            contents = null;
            return;
        }
        if (!lockedSlotsSent) {
            Services.PLATFORM.sendToServer(new Payloads.LockedSlots(EeiConfig.get().lockedSlots));
            lockedSlotsSent = true;
        }
        updateTarget(minecraft);
    }

    private static void updateTarget(Minecraft minecraft) {
        BlockPos newTarget = null;
        BlockPos newBlock = null;
        EeiConfig config = EeiConfig.get();
        if (config.hologramEnabled && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && minecraft.player.getEyePosition().distanceTo(hit.getLocation()) <= config.hologramMaxDistance) {
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

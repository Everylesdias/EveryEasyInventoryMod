package fr.every.everyeasyinventory.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * État du mod pour chaque joueur connecté, côté serveur. Oublié tout seul à la déconnexion.
 */
public final class PlayerState {

    private static final Map<ServerPlayer, PlayerState> STATES = new WeakHashMap<>();

    /** Cases verrouillées envoyées par le client (bit i = case i de l'inventaire). */
    public long lockedSlots;

    /** Conteneur visé pour l'hologramme, et empreinte du dernier résumé envoyé. */
    public @Nullable BlockPos peekPos;
    public int peekHash;

    /** Pour le remplissage auto de la hotbar : ce qui était en main au tick précédent. */
    public int lastSelectedSlot = -1;
    public ItemStack lastHeld = ItemStack.EMPTY;
    public int lastHeldTotal;

    public long lastDepositTick = -1000L;

    public static synchronized PlayerState of(ServerPlayer player) {
        return STATES.computeIfAbsent(player, p -> new PlayerState());
    }

    public boolean isLocked(int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot < 64 && (lockedSlots & (1L << inventorySlot)) != 0;
    }
}

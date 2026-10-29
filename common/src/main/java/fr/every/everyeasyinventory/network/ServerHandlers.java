package fr.every.everyeasyinventory.network;

import fr.every.everyeasyinventory.logic.PlayerState;
import fr.every.everyeasyinventory.logic.ServerActions;
import net.minecraft.server.level.ServerPlayer;

/**
 * Réception des paquets client → serveur. Appelé sur le thread du serveur par chaque loader.
 */
public final class ServerHandlers {

    private ServerHandlers() {
    }

    public static void onSort(Payloads.Sort payload, ServerPlayer player) {
        ServerActions.sort(player, payload.containerId(), payload.playerSide());
    }

    public static void onAreaDeposit(Payloads.AreaDeposit payload, ServerPlayer player) {
        ServerActions.areaDeposit(player);
    }

    public static void onLockedSlots(Payloads.LockedSlots payload, ServerPlayer player) {
        PlayerState.of(player).lockedSlots = payload.mask();
    }

    public static void onPeek(Payloads.Peek payload, ServerPlayer player) {
        ServerActions.peek(player, payload.pos());
    }

    public static void onSearch(Payloads.Search payload, ServerPlayer player) {
        ServerActions.search(player, payload.item().map(holder -> holder.value()));
    }

    public static void onRename(Payloads.Rename payload, ServerPlayer player) {
        ServerActions.rename(player, payload.containerId(), payload.pos(), payload.name());
    }
}

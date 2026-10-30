package fr.every.everyeasyinventory.network;

import fr.every.everyeasyinventory.logic.PlayerState;
import fr.every.everyeasyinventory.config.EeiSetting;
import fr.every.everyeasyinventory.logic.ServerActions;
import fr.every.everyeasyinventory.logic.ServerSettings;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

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

    /** Garde les réglages valides du joueur et lui répond avec ceux que le serveur force. */
    public static void onClientSettings(Payloads.ClientSettings payload, ServerPlayer player) {
        Map<EeiSetting, Object> settings = PlayerState.of(player).clientSettings;
        settings.clear();
        payload.values().forEach((key, text) -> {
            EeiSetting setting = EeiSetting.byKey(key);
            Object value = setting == null ? null : setting.parse(text);
            if (value != null) {
                settings.put(setting, value);
            }
        });
        ServerSettings.sendTo(player);
    }
}

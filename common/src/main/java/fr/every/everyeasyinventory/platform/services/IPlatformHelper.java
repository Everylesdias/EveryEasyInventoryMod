package fr.every.everyeasyinventory.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/**
 * Ce que le code commun demande au loader (Fabric ou NeoForge).
 */
public interface IPlatformHelper {

    Path getConfigDir();

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /**
     * Côté client uniquement.
     */
    void sendToServer(CustomPacketPayload payload);

    /**
     * Côté client uniquement : le serveur connecté accepte-t-il ce paquet (mod présent côté serveur) ?
     */
    boolean canSendToServer(CustomPacketPayload.Type<?> type);
}

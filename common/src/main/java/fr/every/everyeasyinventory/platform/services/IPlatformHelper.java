package fr.every.everyeasyinventory.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import fr.every.everyeasyinventory.logic.EeiStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Ce que le code commun demande au loader (Fabric ou NeoForge).
 */
public interface IPlatformHelper {

    Path getConfigDir();

    /** Stockage exposé par le système d'inventaire du loader (Transfer API, capacités), ou {@code null}. */
    @Nullable EeiStorage findStorage(Level level, BlockPos pos);

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

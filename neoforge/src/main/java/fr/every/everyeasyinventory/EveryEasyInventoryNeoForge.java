package fr.every.everyeasyinventory;

import fr.every.everyeasyinventory.client.ClientState;
import fr.every.everyeasyinventory.logic.EeiCommands;
import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.network.ServerHandlers;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Constants.MOD_ID)
public class EveryEasyInventoryNeoForge {

    public EveryEasyInventoryNeoForge(IEventBus modBus) {
        modBus.addListener(EveryEasyInventoryNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> EeiCommands.register(event.getDispatcher()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Paquets facultatifs : un client avec le mod peut rejoindre un serveur sans lui, et inversement.
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(Payloads.Sort.TYPE, Payloads.Sort.CODEC,
                (payload, context) -> ServerHandlers.onSort(payload, (ServerPlayer) context.player()));
        registrar.playToServer(Payloads.AreaDeposit.TYPE, Payloads.AreaDeposit.CODEC,
                (payload, context) -> ServerHandlers.onAreaDeposit(payload, (ServerPlayer) context.player()));
        registrar.playToServer(Payloads.LockedSlots.TYPE, Payloads.LockedSlots.CODEC,
                (payload, context) -> ServerHandlers.onLockedSlots(payload, (ServerPlayer) context.player()));
        registrar.playToServer(Payloads.Peek.TYPE, Payloads.Peek.CODEC,
                (payload, context) -> ServerHandlers.onPeek(payload, (ServerPlayer) context.player()));
        registrar.playToServer(Payloads.Search.TYPE, Payloads.Search.CODEC,
                (payload, context) -> ServerHandlers.onSearch(payload, (ServerPlayer) context.player()));
        registrar.playToServer(Payloads.Rename.TYPE, Payloads.Rename.CODEC,
                (payload, context) -> ServerHandlers.onRename(payload, (ServerPlayer) context.player()));
        registrar.playToServer(Payloads.QuickFill.TYPE, Payloads.QuickFill.CODEC,
                (payload, context) -> ServerHandlers.onQuickFill(payload, (ServerPlayer) context.player()));
        registrar.playToClient(Payloads.ContainerRules.TYPE, Payloads.ContainerRules.CODEC,
                (payload, context) -> ClientState.onContainerRules(payload));
        registrar.playToServer(Payloads.ClientSettings.TYPE, Payloads.ClientSettings.CODEC,
                (payload, context) -> ServerHandlers.onClientSettings(payload, (ServerPlayer) context.player()));
        registrar.playToClient(Payloads.ForcedSettings.TYPE, Payloads.ForcedSettings.CODEC,
                (payload, context) -> ClientState.onForcedSettings(payload));
        registrar.playToClient(Payloads.SearchResults.TYPE, Payloads.SearchResults.CODEC,
                (payload, context) -> ClientState.onSearchResults(payload));
        registrar.playToClient(Payloads.PeekContents.TYPE, Payloads.PeekContents.CODEC,
                (payload, context) -> ClientState.onPeekContents(payload));
    }
}

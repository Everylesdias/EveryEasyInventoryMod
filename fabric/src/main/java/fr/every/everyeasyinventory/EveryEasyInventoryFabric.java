package fr.every.everyeasyinventory;

import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.network.ServerHandlers;
import net.fabricmc.api.ModInitializer;
import fr.every.everyeasyinventory.logic.EeiCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class EveryEasyInventoryFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Sort.TYPE, Payloads.Sort.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.AreaDeposit.TYPE, Payloads.AreaDeposit.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.LockedSlots.TYPE, Payloads.LockedSlots.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Peek.TYPE, Payloads.Peek.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Search.TYPE, Payloads.Search.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Rename.TYPE, Payloads.Rename.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.ClientSettings.TYPE, Payloads.ClientSettings.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.ForcedSettings.TYPE, Payloads.ForcedSettings.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.PeekContents.TYPE, Payloads.PeekContents.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.SearchResults.TYPE, Payloads.SearchResults.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Payloads.Sort.TYPE, (payload, context) -> ServerHandlers.onSort(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.AreaDeposit.TYPE, (payload, context) -> ServerHandlers.onAreaDeposit(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.LockedSlots.TYPE, (payload, context) -> ServerHandlers.onLockedSlots(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.Peek.TYPE, (payload, context) -> ServerHandlers.onPeek(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.Search.TYPE, (payload, context) -> ServerHandlers.onSearch(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.Rename.TYPE, (payload, context) -> ServerHandlers.onRename(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.ClientSettings.TYPE, (payload, context) -> ServerHandlers.onClientSettings(payload, context.player()));

        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> EeiCommands.register(dispatcher));
    }
}

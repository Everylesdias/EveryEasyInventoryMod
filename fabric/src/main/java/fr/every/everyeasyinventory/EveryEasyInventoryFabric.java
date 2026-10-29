package fr.every.everyeasyinventory;

import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.network.ServerHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class EveryEasyInventoryFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Sort.TYPE, Payloads.Sort.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.AreaDeposit.TYPE, Payloads.AreaDeposit.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.LockedSlots.TYPE, Payloads.LockedSlots.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Peek.TYPE, Payloads.Peek.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.PeekContents.TYPE, Payloads.PeekContents.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Payloads.Sort.TYPE, (payload, context) -> ServerHandlers.onSort(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.AreaDeposit.TYPE, (payload, context) -> ServerHandlers.onAreaDeposit(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.LockedSlots.TYPE, (payload, context) -> ServerHandlers.onLockedSlots(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.Peek.TYPE, (payload, context) -> ServerHandlers.onPeek(payload, context.player()));
    }
}

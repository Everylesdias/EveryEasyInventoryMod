package fr.every.everyeasyinventory;

import fr.every.everyeasyinventory.client.ClientState;
import fr.every.everyeasyinventory.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EveryEasyInventoryFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.PeekContents.TYPE, (payload, context) -> ClientState.onPeekContents(payload));
    }
}

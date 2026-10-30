package fr.every.everyeasyinventory;

import fr.every.everyeasyinventory.client.ClientState;
import fr.every.everyeasyinventory.client.Keys;
import fr.every.everyeasyinventory.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EveryEasyInventoryFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.PeekContents.TYPE, (payload, context) -> ClientState.onPeekContents(payload));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.SearchResults.TYPE, (payload, context) -> ClientState.onSearchResults(payload));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ForcedSettings.TYPE, (payload, context) -> ClientState.onForcedSettings(payload));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ContainerRules.TYPE, (payload, context) -> ClientState.onContainerRules(payload));
        KeyMappingHelper.registerKeyMapping(Keys.SORT);
    }
}

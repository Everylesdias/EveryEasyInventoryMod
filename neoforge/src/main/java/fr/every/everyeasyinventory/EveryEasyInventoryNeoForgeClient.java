package fr.every.everyeasyinventory;

import fr.every.everyeasyinventory.client.EeiSettingsScreen;
import fr.every.everyeasyinventory.client.Keys;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class EveryEasyInventoryNeoForgeClient {

    public EveryEasyInventoryNeoForgeClient(IEventBus modBus, ModContainer container) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(Keys.SORT));
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new EeiSettingsScreen(parent));
    }
}

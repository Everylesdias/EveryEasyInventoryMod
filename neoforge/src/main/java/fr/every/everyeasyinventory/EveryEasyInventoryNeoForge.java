package fr.every.everyeasyinventory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class EveryEasyInventoryNeoForge {

    public EveryEasyInventoryNeoForge(IEventBus modBus) {
        Constants.LOG.info("EveryEasyInventoryMod (NeoForge)");
    }
}

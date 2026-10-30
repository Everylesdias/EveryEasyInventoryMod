package fr.every.everyeasyinventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Touches du mod, réglables dans Options → Commandes. Chaque loader les enregistre à son démarrage.
 */
public final class Keys {

    public static final KeyMapping SORT = new KeyMapping("key.everyeasyinventorymod.sort",
            InputConstants.KEY_R, KeyMapping.Category.INVENTORY);

    private Keys() {
    }
}

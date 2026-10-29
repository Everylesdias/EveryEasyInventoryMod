package fr.every.everyeasyinventory.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Pour afficher tout de suite le nouveau nom d'un conteneur renommé, sans rouvrir l'écran. */
@Mixin(Screen.class)
public interface ScreenAccessor {

    @Mutable
    @Accessor("title")
    void eei$setTitle(Component title);
}

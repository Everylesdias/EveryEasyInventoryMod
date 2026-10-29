package fr.every.everyeasyinventory.mixin;

import fr.every.everyeasyinventory.client.HologramRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * L'hologramme est dessiné en premier dans le HUD, sous le réticule et la hotbar.
 */
@Mixin(Hud.class)
public abstract class HudMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow
    public abstract boolean isHidden();

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void eei$renderHologram(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!isHidden() && minecraft.gui.screen() == null) {
            HologramRenderer.render(graphics, minecraft);
        }
    }
}

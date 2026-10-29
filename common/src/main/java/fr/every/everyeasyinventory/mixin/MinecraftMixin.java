package fr.every.everyeasyinventory.mixin;

import fr.every.everyeasyinventory.client.ClientState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void eei$tick(CallbackInfo ci) {
        ClientState.tick((Minecraft) (Object) this);
    }
}

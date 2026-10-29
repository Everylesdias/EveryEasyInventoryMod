package fr.every.everyeasyinventory.mixin;

import fr.every.everyeasyinventory.logic.ServerActions;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void eei$tick(CallbackInfo ci) {
        ServerActions.tick((ServerPlayer) (Object) this);
    }
}

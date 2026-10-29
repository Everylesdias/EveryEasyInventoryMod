package fr.every.everyeasyinventory.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Le nom personnalisé d'un conteneur n'a pas de setter : seule l'enclume (via l'item) le pose. */
@Mixin(BaseContainerBlockEntity.class)
public interface BaseContainerBlockEntityAccessor {

    @Accessor("name")
    void eei$setName(@Nullable Component name);
}

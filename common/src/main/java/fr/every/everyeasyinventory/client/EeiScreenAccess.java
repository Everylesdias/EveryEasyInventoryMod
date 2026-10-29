package fr.every.everyeasyinventory.client;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/**
 * Accès aux champs protégés d'un écran de conteneur, ajouté par mixin à {@code AbstractContainerScreen}.
 */
public interface EeiScreenAccess {

    AbstractContainerMenu eei$menu();

    @Nullable Slot eei$hoveredSlot();

    /** Case sous ce point de l'écran, sans attendre la prochaine image. */
    @Nullable Slot eei$slotAt(double x, double y);

    int eei$leftPos();

    int eei$topPos();

    int eei$imageWidth();

    int eei$titleLabelY();

    int eei$inventoryLabelY();

    /** Clic de case envoyé au serveur comme un vrai clic du joueur (prédit localement). */
    void eei$click(Slot slot, int button, ContainerInput input);
}

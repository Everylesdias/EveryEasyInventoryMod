package fr.every.everyeasyinventory.logic;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Un stockage, quelle que soit la façon dont son mod l'expose : inventaire du jeu ({@link Container}),
 * Transfer API de Fabric ou capacités de NeoForge. Les piles lues ne doivent pas être modifiées.
 */
public interface EeiStorage {

    int size();

    ItemStack get(int slot);

    /** Complète les piles identiques ; retire de {@code stack} ce qui est entré. Vrai si quelque chose a bougé. */
    boolean fillExisting(ItemStack stack);

    /** Met {@code stack} dans les cases vides ; retire de {@code stack} ce qui est entré. Vrai si quelque chose a bougé. */
    boolean fillEmpty(ItemStack stack);

    /** Prend les items identiques qui manquent à {@code stack} et les lui ajoute ; renvoie le nombre pris. */
    int takeInto(ItemStack stack, Inventory into);

    /** Assez de cases, ou des cases à grande capacité (caisses) : un stockage, pas une machine. */
    boolean looksLikeStorage();

    void setChanged();

    /** Inventaire du jeu. */
    record OfContainer(Container container) implements EeiStorage {

        @Override
        public int size() {
            return container.getContainerSize();
        }

        @Override
        public ItemStack get(int slot) {
            return container.getItem(slot);
        }

        @Override
        public boolean fillExisting(ItemStack stack) {
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
                ItemStack existing = container.getItem(i);
                int max = Math.min(container.getMaxStackSize(), existing.getMaxStackSize());
                if (!existing.isEmpty() && existing.getCount() < max && ItemStack.isSameItemSameComponents(existing, stack)
                        && container.canPlaceItem(i, stack)) {
                    int count = Math.min(max - existing.getCount(), stack.getCount());
                    existing.grow(count);
                    stack.shrink(count);
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean fillEmpty(ItemStack stack) {
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
                if (container.getItem(i).isEmpty() && container.canPlaceItem(i, stack)) {
                    int max = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
                    container.setItem(i, stack.split(Math.min(max, stack.getCount())));
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public int takeInto(ItemStack stack, Inventory into) {
            int total = 0;
            for (int i = 0; i < container.getContainerSize() && stack.getCount() < stack.getMaxStackSize(); i++) {
                ItemStack source = container.getItem(i);
                if (!source.isEmpty() && ItemStack.isSameItemSameComponents(source, stack) && container.canTakeItem(into, i, source)) {
                    int count = Math.min(stack.getMaxStackSize() - stack.getCount(), source.getCount());
                    container.removeItem(i, count);
                    stack.grow(count);
                    total += count;
                }
            }
            return total;
        }

        @Override
        public boolean looksLikeStorage() {
            return container.getContainerSize() >= 9;
        }

        @Override
        public void setChanged() {
            container.setChanged();
        }
    }
}

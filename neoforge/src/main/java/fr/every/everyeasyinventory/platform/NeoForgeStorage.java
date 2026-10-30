package fr.every.everyeasyinventory.platform;

import fr.every.everyeasyinventory.logic.EeiStorage;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Stockage d'un autre mod exposé par les capacités de NeoForge. */
record NeoForgeStorage(ResourceHandler<ItemResource> handler) implements EeiStorage {

    private static final int BULK_CAPACITY = 100;

    @Override
    public int size() {
        return handler.size();
    }

    @Override
    public ItemStack get(int slot) {
        ItemResource resource = handler.getResource(slot);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot));
    }

    private boolean fill(ItemStack stack, boolean intoEmpty) {
        ItemResource resource = ItemResource.of(stack);
        boolean changed = false;
        for (int i = 0; i < handler.size() && !stack.isEmpty(); i++) {
            ItemResource current = handler.getResource(i);
            if (intoEmpty ? !current.isEmpty() : current.isEmpty() || !current.equals(resource)) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(i, resource, stack.getCount(), transaction);
                if (inserted > 0) {
                    transaction.commit();
                    stack.shrink(inserted);
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    public boolean fillExisting(ItemStack stack) {
        return fill(stack, false);
    }

    @Override
    public boolean fillEmpty(ItemStack stack) {
        return fill(stack, true);
    }

    @Override
    public int takeInto(ItemStack stack, Inventory into) {
        ItemResource resource = ItemResource.of(stack);
        int total = 0;
        for (int i = 0; i < handler.size() && stack.getCount() < stack.getMaxStackSize(); i++) {
            if (!handler.getResource(i).equals(resource)) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(i, resource, stack.getMaxStackSize() - stack.getCount(), transaction);
                if (extracted > 0) {
                    transaction.commit();
                    stack.grow(extracted);
                    total += extracted;
                }
            }
        }
        return total;
    }

    @Override
    public boolean looksLikeStorage() {
        if (handler.size() >= 9) {
            return true;
        }
        for (int i = 0; i < handler.size(); i++) {
            if (handler.getCapacityAsInt(i, handler.getResource(i)) >= BULK_CAPACITY) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setChanged() {
        // La transaction a déjà prévenu le bloc.
    }
}

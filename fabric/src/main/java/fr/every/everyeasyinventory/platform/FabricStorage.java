package fr.every.everyeasyinventory.platform;

import fr.every.everyeasyinventory.logic.EeiStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Stockage d'un autre mod exposé par la Transfer API de Fabric. */
record FabricStorage(SlottedStorage<ItemVariant> storage) implements EeiStorage {

    private static final int BULK_CAPACITY = 100;

    @Override
    public int size() {
        return storage.getSlotCount();
    }

    @Override
    public ItemStack get(int slot) {
        SingleSlotStorage<ItemVariant> view = storage.getSlot(slot);
        return view.isResourceBlank() ? ItemStack.EMPTY : view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
    }

    private boolean fill(ItemStack stack, boolean intoEmpty) {
        ItemVariant variant = ItemVariant.of(stack);
        boolean changed = false;
        for (int i = 0; i < storage.getSlotCount() && !stack.isEmpty(); i++) {
            SingleSlotStorage<ItemVariant> slot = storage.getSlot(i);
            if (intoEmpty ? !slot.isResourceBlank() : slot.isResourceBlank() || !slot.getResource().equals(variant)) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = slot.insert(variant, stack.getCount(), transaction);
                if (inserted > 0) {
                    transaction.commit();
                    stack.shrink((int) inserted);
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
        ItemVariant variant = ItemVariant.of(stack);
        int total = 0;
        for (int i = 0; i < storage.getSlotCount() && stack.getCount() < stack.getMaxStackSize(); i++) {
            SingleSlotStorage<ItemVariant> slot = storage.getSlot(i);
            if (slot.isResourceBlank() || !slot.getResource().equals(variant)) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                int extracted = (int) slot.extract(variant, stack.getMaxStackSize() - stack.getCount(), transaction);
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
        if (storage.getSlotCount() >= 9) {
            return true;
        }
        for (int i = 0; i < storage.getSlotCount(); i++) {
            if (storage.getSlot(i).getCapacity() >= BULK_CAPACITY) {
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

package fr.every.everyeasyinventory.logic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fusion des piles incomplètes et ordre de tri.
 */
public final class ItemSorting {

    /**
     * Ordre d'enregistrement des items (la liste vanilla, qui suit de près les onglets du mode créatif),
     * puis nom affiché, puis la plus grosse pile d'abord, puis la moins usée d'abord.
     */
    public static final Comparator<ItemStack> ORDER = Comparator
            .<ItemStack>comparingInt(stack -> BuiltInRegistries.ITEM.getId(stack.getItem()))
            .thenComparing(stack -> stack.getHoverName().getString())
            .thenComparing(ItemStack::getCount, Comparator.reverseOrder())
            .thenComparingInt(stack -> stack.getOrDefault(DataComponents.DAMAGE, 0));

    private ItemSorting() {
    }

    /**
     * Regroupe les piles identiques (même item, mêmes composants) jusqu'à leur taille maximale, puis trie.
     * Les piles d'entrée ne sont pas modifiées.
     */
    public static List<ItemStack> mergeAndSort(List<ItemStack> stacks, int slotLimit) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = stack.copy();
            for (ItemStack target : merged) {
                if (remaining.isEmpty()) {
                    break;
                }
                int max = Math.min(slotLimit, target.getMaxStackSize());
                if (target.getCount() < max && ItemStack.isSameItemSameComponents(target, remaining)) {
                    int moved = Math.min(max - target.getCount(), remaining.getCount());
                    target.grow(moved);
                    remaining.shrink(moved);
                }
            }
            if (!remaining.isEmpty()) {
                merged.add(remaining);
            }
        }
        merged.sort(ORDER);
        return merged;
    }
}

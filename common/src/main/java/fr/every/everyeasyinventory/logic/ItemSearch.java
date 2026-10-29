package fr.every.everyeasyinventory.logic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.HashSet;
import java.util.Set;

/**
 * Recherche d'item, commune au client et au serveur : un item cherché trouve tout son groupe
 * (mêmes règles que l'hologramme), y compris dans les shulkers et autres objets qui contiennent des items.
 */
public final class ItemSearch {

    private ItemSearch() {
    }

    /** Tous les items du groupe de cet item, calculé une fois par recherche pour des tests rapides ensuite. */
    public static Set<Item> members(Item item) {
        Item group = ServerActions.groupOf(item);
        Set<Item> members = new HashSet<>();
        for (Item candidate : BuiltInRegistries.ITEM) {
            if (candidate != Items.AIR && ServerActions.groupOf(candidate) == group) {
                members.add(candidate);
            }
        }
        return members;
    }

    /** La pile est-elle de ce groupe, ou en contient-elle (shulker, etc.) ? Pas de récursion plus profonde. */
    public static boolean matches(ItemStack stack, Set<Item> members) {
        if (stack.isEmpty()) {
            return false;
        }
        if (members.contains(stack.getItem())) {
            return true;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            for (ItemStackTemplate inner : contents.nonEmptyItems()) {
                if (members.contains(inner.item().value())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean contains(Container container, Set<Item> members) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (matches(container.getItem(i), members)) {
                return true;
            }
        }
        return false;
    }
}

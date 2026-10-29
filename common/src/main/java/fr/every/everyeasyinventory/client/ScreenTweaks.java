package fr.every.everyeasyinventory.client;

import fr.every.everyeasyinventory.config.EeiConfig;
import fr.every.everyeasyinventory.logic.ServerActions;
import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Boutons et raccourcis souris des écrans d'inventaire et de conteneur.
 * Les transferts passent par de vrais clics de case : ils marchent aussi sur un serveur sans le mod.
 */
public final class ScreenTweaks {

    private static final int BUTTON_SIZE = 11;
    private static final int LOCK_TINT = 0x40FF4040;
    private static final int LOCK_COLOR = 0xFFE8C040;
    private static final int LOCK_SHADOW = 0xFF3F2F10;

    /** Bouton et sa position relative au coin haut gauche de l'image de l'écran. */
    public record Placed(Button button, int dx, int dy) {
    }

    /** Cases déjà transférées pendant un glisser avec Maj. */
    private static final Set<Slot> DRAGGED = new HashSet<>();

    private ScreenTweaks() {
    }

    public static boolean isExcluded(Screen screen) {
        return screen instanceof CreativeModeInventoryScreen;
    }

    // ---------------------------------------------------------------- boutons

    public static List<Placed> createButtons(Screen screen, EeiScreenAccess access) {
        List<Placed> buttons = new ArrayList<>();
        AbstractContainerMenu menu = access.eei$menu();
        boolean server = ClientState.serverHasMod();
        int right = access.eei$imageWidth() - 7 - BUTTON_SIZE;

        if (screen instanceof InventoryScreen) {
            if (server) {
                Component label = Component.translatable("button.everyeasyinventorymod.send");
                int width = Minecraft.getInstance().font.width(label) + 8;
                buttons.add(new Placed(Button.builder(label, b -> Services.PLATFORM.sendToServer(new Payloads.AreaDeposit()))
                        .size(width, 12)
                        .tooltip(Tooltip.create(Component.translatable("button.everyeasyinventorymod.send.tooltip")))
                        .build(), 0, -14));
                buttons.add(new Placed(small("⇅", "sort_inventory", b -> sort(access, true)), access.eei$imageWidth() - 13, -14));
            }
            return buttons;
        }
        if (!ServerActions.isSortableMenu(menu)) {
            return buttons;
        }
        int containerRow = access.eei$titleLabelY() - 2;
        int playerRow = access.eei$inventoryLabelY() - 2;
        int x = right;
        if (server) {
            buttons.add(new Placed(small("⇅", "sort_container", b -> sort(access, false)), x, containerRow));
            x -= BUTTON_SIZE + 1;
        }
        buttons.add(new Placed(small("↓", "take_all", b -> takeAll(access)), x, containerRow));

        x = right;
        if (server) {
            buttons.add(new Placed(small("⇅", "sort_inventory", b -> sort(access, true)), x, playerRow));
            x -= BUTTON_SIZE + 1;
        }
        buttons.add(new Placed(small("⇡", "deposit_matching", b -> deposit(access, true)), x, playerRow));
        x -= BUTTON_SIZE + 1;
        buttons.add(new Placed(small("↑", "deposit_all", b -> deposit(access, false)), x, playerRow));
        return buttons;
    }

    private static Button small(String glyph, String key, Button.OnPress onPress) {
        return Button.builder(Component.literal(glyph), onPress)
                .size(BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("button.everyeasyinventorymod." + key)))
                .build();
    }

    // ---------------------------------------------------------------- actions

    private static void sort(EeiScreenAccess access, boolean playerSide) {
        Services.PLATFORM.sendToServer(new Payloads.Sort(access.eei$menu().containerId, playerSide));
    }

    private static void takeAll(EeiScreenAccess access) {
        for (Slot slot : access.eei$menu().slots) {
            if (!isPlayerSlot(slot) && slot.hasItem()) {
                access.eei$click(slot, 0, ContainerInput.QUICK_MOVE);
            }
        }
    }

    /** Inventaire principal (hotbar et cases verrouillées exclues) → conteneur. */
    private static void deposit(EeiScreenAccess access, boolean onlyMatching) {
        AbstractContainerMenu menu = access.eei$menu();
        Set<Item> present = new HashSet<>();
        for (Slot slot : menu.slots) {
            if (!isPlayerSlot(slot) && slot.hasItem()) {
                present.add(slot.getItem().getItem());
            }
        }
        for (Slot slot : menu.slots) {
            int index = slot.getContainerSlot();
            if (isPlayerSlot(slot) && index >= Inventory.getSelectionSize() && slot.hasItem() && !ClientState.isLocked(index)
                    && (!onlyMatching || present.contains(slot.getItem().getItem()))) {
                access.eei$click(slot, 0, ContainerInput.QUICK_MOVE);
            }
        }
    }

    // ---------------------------------------------------------------- souris et clavier

    /** @return vrai si l'événement est consommé */
    public static boolean mouseClicked(EeiScreenAccess access, MouseButtonEvent event) {
        Slot slot = access.eei$hoveredSlot();
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || slot == null) {
            return false;
        }
        if (alt(event) && isPlayerSlot(slot)) {
            ClientState.toggleLock(slot.getContainerSlot());
            return true;
        }
        boolean carrying = !access.eei$menu().getCarried().isEmpty();
        if (shift(event) && control(event) && slot.hasItem() && !carrying) {
            Item item = slot.getItem().getItem();
            for (Slot other : access.eei$menu().slots) {
                if (other.container == slot.container && other.hasItem() && other.getItem().getItem() == item
                        && !(isPlayerSlot(other) && ClientState.isLocked(other.getContainerSlot()))) {
                    access.eei$click(other, 0, ContainerInput.QUICK_MOVE);
                }
            }
            return true;
        }
        if (shift(event)) {
            DRAGGED.clear();
            DRAGGED.add(slot);
        }
        return false;
    }

    public static boolean mouseDragged(EeiScreenAccess access, MouseButtonEvent event) {
        Slot slot = access.eei$slotAt(event.x(), event.y());
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !shift(event)
                || slot == null || !access.eei$menu().getCarried().isEmpty()) {
            return false;
        }
        if (slot.hasItem() && DRAGGED.add(slot)) {
            access.eei$click(slot, 0, ContainerInput.QUICK_MOVE);
        }
        return true;
    }

    public static void mouseReleased() {
        DRAGGED.clear();
    }

    /**
     * Molette vers le bas : envoie un item de la case survolée de l'autre côté.
     * Molette vers le haut : ramène un item identique de l'autre côté dans la case survolée.
     */
    public static boolean mouseScrolled(EeiScreenAccess access, double x, double y, double scrollY) {
        AbstractContainerMenu menu = access.eei$menu();
        Slot slot = access.eei$slotAt(x, y);
        if (scrollY == 0 || slot == null || !slot.hasItem() || !menu.getCarried().isEmpty()
                || !ServerActions.isSortableMenu(menu) || slot.getItem().has(DataComponents.BUNDLE_CONTENTS)) {
            return false;
        }
        boolean fromPlayer = isPlayerSlot(slot);
        Predicate<Slot> otherSide = other -> isPlayerSlot(other) != fromPlayer;
        ItemStack stack = slot.getItem();
        if (scrollY < 0) {
            Slot target = findPlace(menu, otherSide, stack);
            if (target != null) {
                moveOne(access, slot, target);
            }
        } else if (stack.getCount() < slot.getMaxStackSize(stack)) {
            for (Slot source : menu.slots) {
                if (otherSide.test(source) && source.hasItem() && ItemStack.isSameItemSameComponents(source.getItem(), stack)) {
                    moveOne(access, source, slot);
                    break;
                }
            }
        }
        return true;
    }

    private static @Nullable Slot findPlace(AbstractContainerMenu menu, Predicate<Slot> side, ItemStack stack) {
        Slot empty = null;
        for (Slot candidate : menu.slots) {
            if (!side.test(candidate) || !candidate.mayPlace(stack)) {
                continue;
            }
            ItemStack existing = candidate.getItem();
            if (existing.isEmpty()) {
                if (empty == null) {
                    empty = candidate;
                }
            } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < candidate.getMaxStackSize(existing)) {
                return candidate;
            }
        }
        return empty;
    }

    /** Prendre la pile, poser un item (clic droit), reposer le reste. */
    private static void moveOne(EeiScreenAccess access, Slot from, Slot to) {
        access.eei$click(from, 0, ContainerInput.PICKUP);
        access.eei$click(to, 1, ContainerInput.PICKUP);
        access.eei$click(from, 0, ContainerInput.PICKUP);
    }

    public static boolean keyPressed(EeiScreenAccess access, int key) {
        if (key != InputConstants.KEY_R || !ClientState.serverHasMod()) {
            return false;
        }
        Slot slot = access.eei$hoveredSlot();
        AbstractContainerMenu menu = access.eei$menu();
        boolean playerSide = slot == null || isPlayerSlot(slot);
        if (!playerSide && !ServerActions.isSortableMenu(menu)) {
            return false;
        }
        sort(access, playerSide);
        return true;
    }

    // ---------------------------------------------------------------- cases verrouillées

    /** Dessiné dans le repère de l'image de l'écran (après le décalage leftPos / topPos). */
    public static void drawLocks(EeiScreenAccess access, GuiGraphicsExtractor graphics) {
        if (EeiConfig.get().lockedSlots == 0) {
            return;
        }
        for (Slot slot : access.eei$menu().slots) {
            if (slot.isActive() && isPlayerSlot(slot) && ClientState.isLocked(slot.getContainerSlot())) {
                int x = slot.x;
                int y = slot.y;
                graphics.fill(x, y, x + 16, y + 16, LOCK_TINT);
                // Petit cadenas dans le coin haut gauche : anse puis corps.
                graphics.fill(x + 1, y, x + 6, y + 4, LOCK_SHADOW);
                graphics.fill(x + 2, y + 1, x + 3, y + 3, LOCK_COLOR);
                graphics.fill(x + 4, y + 1, x + 5, y + 3, LOCK_COLOR);
                graphics.fill(x, y + 3, x + 7, y + 8, LOCK_SHADOW);
                graphics.fill(x + 1, y + 4, x + 6, y + 7, LOCK_COLOR);
            }
        }
    }

    // L'événement porte les touches de modification ; l'état du clavier sert de secours (entrées simulées).
    private static boolean shift(MouseButtonEvent event) {
        return event.hasShiftDown() || Minecraft.getInstance().hasShiftDown();
    }

    private static boolean control(MouseButtonEvent event) {
        return event.hasControlDown() || Minecraft.getInstance().hasControlDown();
    }

    private static boolean alt(MouseButtonEvent event) {
        return event.hasAltDown() || Minecraft.getInstance().hasAltDown();
    }

    /** Case de l'inventaire du joueur (hotbar ou inventaire principal, pas l'armure). */
    public static boolean isPlayerSlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() < 36;
    }
}

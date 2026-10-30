package fr.every.everyeasyinventory.mixin;

import fr.every.everyeasyinventory.client.EeiScreenAccess;
import fr.every.everyeasyinventory.client.RenameField;
import fr.every.everyeasyinventory.client.ScreenTweaks;
import fr.every.everyeasyinventory.client.SearchBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen implements EeiScreenAccess {

    @Shadow @Final protected T menu;
    @Shadow protected @Nullable Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;
    @Shadow protected int titleLabelX;
    @Shadow protected int titleLabelY;
    @Shadow protected int inventoryLabelY;

    @Shadow
    private @Nullable Slot getHoveredSlot(double x, double y) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput);

    @Unique
    private final List<ScreenTweaks.Placed> eei$buttons = new ArrayList<>();
    @Unique
    private @Nullable SearchBar eei$searchBar;
    @Unique
    private @Nullable RenameField eei$renameField;
    /** Le conteneur est identifié à la première ouverture (init est rappelé quand la fenêtre change de taille). */
    @Unique
    private boolean eei$renameChecked;
    @Unique
    private @Nullable RenameField eei$renameCandidate;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void eei$addButtons(CallbackInfo ci) {
        eei$buttons.clear();
        eei$searchBar = null;
        if (ScreenTweaks.isExcluded(this)) {
            return;
        }
        for (ScreenTweaks.Placed placed : ScreenTweaks.createButtons(this, this)) {
            addRenderableWidget(placed.button());
            eei$buttons.add(placed);
        }
        if (ScreenTweaks.hasSearchBar(this)) {
            eei$searchBar = new SearchBar(this);
            addRenderableWidget(eei$searchBar.box());
        }
        if (!eei$renameChecked) {
            eei$renameChecked = true;
            eei$renameCandidate = ScreenTweaks.isRenamable(this)
                    ? RenameField.create(this, this, title -> ((ScreenAccessor) this).eei$setTitle(title)) : null;
        }
        eei$renameField = eei$renameCandidate;
        if (eei$renameField != null) {
            addRenderableWidget(eei$renameField.box());
        }
        eei$layoutButtons();
    }

    /**
     * Le livre de recettes déplace l'écran après init : on recale les boutons à chaque image.
     * {@code extractContents} plutôt que {@code extractRenderState}, que les écrans à livre de recettes n'appellent pas.
     */
    @Inject(method = "extractContents", at = @At("HEAD"))
    private void eei$layoutBeforeRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        eei$layoutButtons();
    }

    @Unique
    private void eei$layoutButtons() {
        for (ScreenTweaks.Placed placed : eei$buttons) {
            placed.button().setPosition(leftPos + placed.dx(), topPos + placed.dy());
        }
        if (eei$searchBar != null) {
            eei$searchBar.layout(leftPos, topPos, imageWidth, imageHeight);
        }
        if (eei$renameField != null) {
            eei$renameField.layout(titleLabelX, ScreenTweaks.titleRowButtonsWidth(this));
        }
    }

    /** Suggestions de la barre de recherche, par-dessus le contenu de l'écran. */
    @Inject(method = "extractContents", at = @At("TAIL"))
    private void eei$drawSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (eei$renameField != null) {
            eei$renameField.extractHover(graphics, mouseX, mouseY);
        }
        if (eei$searchBar != null) {
            eei$searchBar.extractSuggestions(graphics);
        }
    }

    @Inject(method = "extractSlots", at = @At("TAIL"))
    private void eei$drawLocks(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!ScreenTweaks.isExcluded(this)) {
            ScreenTweaks.drawSearch(this, graphics);
            ScreenTweaks.drawLocks(this, graphics);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void eei$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (eei$renameField != null && eei$renameField.mouseClicked(event)) {
            cir.setReturnValue(true);
            return;
        }
        if (eei$searchBar != null && eei$searchBar.mouseClicked(event)) {
            cir.setReturnValue(true);
            return;
        }
        if (!ScreenTweaks.isExcluded(this) && ScreenTweaks.mouseClicked(this, event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void eei$mouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!ScreenTweaks.isExcluded(this) && ScreenTweaks.mouseDragged(this, event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void eei$mouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        ScreenTweaks.mouseReleased();
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void eei$mouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (!ScreenTweaks.isExcluded(this) && ScreenTweaks.mouseScrolled(this, x, y, scrollY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void eei$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (eei$renameField != null && eei$renameField.keyPressed(event)) {
            cir.setReturnValue(true);
            return;
        }
        // Barre de recherche active : les touches tapées ne ferment pas l'écran et ne déclenchent pas de raccourci.
        if (eei$searchBar != null && eei$searchBar.keyPressed(event)) {
            cir.setReturnValue(true);
            return;
        }
        if (!ScreenTweaks.isExcluded(this) && ScreenTweaks.keyPressed(this, event)) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public AbstractContainerMenu eei$menu() {
        return menu;
    }

    @Override
    public @Nullable Slot eei$hoveredSlot() {
        return hoveredSlot;
    }

    @Override
    public @Nullable Slot eei$slotAt(double x, double y) {
        return getHoveredSlot(x, y);
    }

    @Override
    public int eei$leftPos() {
        return leftPos;
    }

    @Override
    public int eei$topPos() {
        return topPos;
    }

    @Override
    public int eei$imageWidth() {
        return imageWidth;
    }

    @Override
    public int eei$titleLabelY() {
        return titleLabelY;
    }

    @Override
    public int eei$inventoryLabelY() {
        return inventoryLabelY;
    }

    @Override
    public void eei$click(Slot slot, int button, ContainerInput input) {
        slotClicked(slot, slot.index, button, input);
    }
}

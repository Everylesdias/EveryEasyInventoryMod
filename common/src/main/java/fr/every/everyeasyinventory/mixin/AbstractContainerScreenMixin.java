package fr.every.everyeasyinventory.mixin;

import fr.every.everyeasyinventory.client.EeiScreenAccess;
import fr.every.everyeasyinventory.client.ScreenTweaks;
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

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void eei$addButtons(CallbackInfo ci) {
        eei$buttons.clear();
        if (ScreenTweaks.isExcluded(this)) {
            return;
        }
        for (ScreenTweaks.Placed placed : ScreenTweaks.createButtons(this, this)) {
            addRenderableWidget(placed.button());
            eei$buttons.add(placed);
        }
        eei$layoutButtons();
    }

    /** Le livre de recettes déplace l'écran après init : on recale les boutons à chaque image. */
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void eei$layoutBeforeRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        eei$layoutButtons();
    }

    @Unique
    private void eei$layoutButtons() {
        for (ScreenTweaks.Placed placed : eei$buttons) {
            placed.button().setPosition(leftPos + placed.dx(), topPos + placed.dy());
        }
    }

    @Inject(method = "extractSlots", at = @At("TAIL"))
    private void eei$drawLocks(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!ScreenTweaks.isExcluded(this)) {
            ScreenTweaks.drawLocks(this, graphics);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void eei$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
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
        if (!ScreenTweaks.isExcluded(this) && ScreenTweaks.mouseScrolled(this, scrollY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void eei$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!ScreenTweaks.isExcluded(this) && ScreenTweaks.keyPressed(this, event.key())) {
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

package fr.every.everyeasyinventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.every.everyeasyinventory.logic.ContainerFinder;
import fr.every.everyeasyinventory.logic.ServerActions;
import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Titre cliquable d'un écran de conteneur : un clic le remplace par un champ de texte pour renommer le conteneur
 * (nom vanilla du bloc, posé par le serveur). Entrée ou un clic ailleurs valide, Échap annule, vide = nom par défaut.
 */
public final class RenameField {

    private static final int PENCIL_COLOR = 0xFF404040;

    private final Screen screen;
    private final EeiScreenAccess access;
    private final BlockPos pos;
    private final Consumer<Component> setTitle;
    private final EditBox box;
    private int titleX;
    private int titleY;
    private int maxWidth;
    /** Titre d'avant l'édition : le jeu dessine le titre par-dessus les champs, on le vide pendant la saisie. */
    private Component savedTitle = Component.empty();

    private RenameField(Screen screen, EeiScreenAccess access, BlockPos pos, Consumer<Component> setTitle) {
        this.screen = screen;
        this.access = access;
        this.pos = pos;
        this.setTitle = setTitle;
        this.box = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 12, Component.translatable("rename.everyeasyinventorymod.narration"));
        box.setMaxLength(ServerActions.MAX_NAME_LENGTH);
        box.setVisible(false);
    }

    /**
     * Champ de renommage si le serveur a le mod et que l'écran vient d'un conteneur géré, visé par le joueur à l'ouverture.
     */
    public static @Nullable RenameField create(Screen screen, EeiScreenAccess access, Consumer<Component> setTitle) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientState.serverHasMod() || minecraft.level == null
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !ContainerFinder.isSupported(minecraft.level.getBlockEntity(hit.getBlockPos()))
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof BaseContainerBlockEntity)) {
            return null;
        }
        return new RenameField(screen, access, hit.getBlockPos(), setTitle);
    }

    public EditBox box() {
        return box;
    }

    public boolean isEditing() {
        return box.isVisible();
    }

    /** @param buttonsWidth place prise par les boutons à droite de la ligne du titre */
    public void layout(int titleLabelX, int buttonsWidth) {
        titleX = access.eei$leftPos() + titleLabelX;
        titleY = access.eei$topPos() + access.eei$titleLabelY();
        maxWidth = access.eei$imageWidth() - titleLabelX - 7 - buttonsWidth - 2;
        box.setRectangle(maxWidth, 12, titleX - 2, titleY - 2);
    }

    private boolean isOverTitle(double x, double y) {
        Font font = Minecraft.getInstance().font;
        int width = Math.min(maxWidth, font.width(screen.getTitle()) + 8);
        return x >= titleX - 1 && x < titleX + width && y >= titleY - 2 && y < titleY + 10;
    }

    // ---------------------------------------------------------------- édition

    private void start() {
        savedTitle = screen.getTitle();
        setTitle.accept(Component.empty());
        box.setValue(ContainerFinder.customName(Minecraft.getInstance().level, pos) != null ? savedTitle.getString() : "");
        box.setHint(savedTitle.copy());
        box.setVisible(true);
        box.setFocused(true);
        screen.setFocused(box);
    }

    private void commit() {
        String name = box.getValue().trim();
        stop();
        Services.PLATFORM.sendToServer(new Payloads.Rename(access.eei$menu().containerId, pos, name));
        setTitle.accept(name.isEmpty() ? defaultName(Minecraft.getInstance().level, pos) : Component.literal(name));
    }

    private void stop() {
        box.setVisible(false);
        box.setFocused(false);
        if (screen.getFocused() == box) {
            screen.setFocused(null);
        }
    }

    /** Titre d'origine du conteneur, comme le jeu l'affiche sans nom personnalisé. */
    private static Component defaultName(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return Component.translatable("container.chestDouble");
        }
        return state.getBlock().getName();
    }

    // ---------------------------------------------------------------- entrées

    /** @return vrai si le clic est consommé (il ouvre l'édition) */
    public boolean mouseClicked(MouseButtonEvent event) {
        if (isEditing()) {
            if (!box.isMouseOver(event.x(), event.y())) {
                commit();
            }
            return false;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isOverTitle(event.x(), event.y())) {
            start();
            return true;
        }
        return false;
    }

    /** @return vrai si la touche est consommée (toutes, pendant l'édition) */
    public boolean keyPressed(KeyEvent event) {
        if (!isEditing()) {
            return false;
        }
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            stop();
            setTitle.accept(savedTitle);
        } else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            commit();
        } else {
            box.keyPressed(event);
        }
        return true;
    }

    // ---------------------------------------------------------------- dessin

    /** Petit crayon après le titre quand la souris le survole. */
    public void extractHover(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isEditing() || !isOverTitle(mouseX, mouseY)) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int x = titleX + Math.min(font.width(screen.getTitle()) + 2, maxWidth - 6);
        int y = titleY + 1;
        for (int i = 0; i < 5; i++) {
            graphics.fill(x + i, y + 5 - i, x + i + 2, y + 6 - i, PENCIL_COLOR);
        }
        graphics.fill(x, y + 6, x + 1, y + 7, PENCIL_COLOR);
    }
}

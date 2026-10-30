package fr.every.everyeasyinventory.client;

import fr.every.everyeasyinventory.config.EeiConfig;
import fr.every.everyeasyinventory.config.EeiSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Réglages du mod. Un réglage forcé par le serveur est affiché avec sa valeur, grisé, avec un cadenas.
 * La touche de tri se change dans Options → Commandes, avec les autres touches du jeu.
 */
public final class EeiSettingsScreen extends Screen {

    private static final int[] PALETTE = {0xFFC21A, 0xFF3B30, 0xFF8C00, 0x34C759, 0x00E5FF, 0x2979FF, 0xFF2DAA, 0xFFFFFF};
    private static final int COLUMN_WIDTH = 150;
    private static final int ROW_HEIGHT = 20;
    private static final int SWATCH_SIZE = 18;
    private static final int LABEL_COLOR = 0xFFE0E0E0;
    private static final int ERROR_COLOR = 0xFFFF5050;
    private static final int TEXT_COLOR = 0xFFE0E0E0;

    private final @Nullable Screen parent;
    /** Boutons de la palette, dans l'ordre de {@link #PALETTE}. */
    private final List<Button> swatches = new ArrayList<>();
    /** Réglages forcés affichés : leur cadenas est dessiné à chaque image. */
    private final List<AbstractWidget> locked = new ArrayList<>();
    private @Nullable EditBox hexBox;
    private int leftX;
    private int rightX;
    private int colorRowY;
    private int searchLabelY;
    private int hologramLabelY;
    private int depositLabelY;

    public EeiSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("settings.everyeasyinventorymod.title"));
        this.parent = parent;
    }

    /** Les réglages forcés viennent de changer : on redessine l'écran avec leur nouvel état. */
    public void refresh() {
        rebuildWidgets();
    }

    @Override
    protected void init() {
        swatches.clear();
        locked.clear();
        leftX = width / 2 - COLUMN_WIDTH - 5;
        rightX = width / 2 + 5;
        int y = 22;

        searchLabelY = y;
        y += 12;
        addToggle(EeiSetting.SEARCH_ENABLED, leftX, y);
        addToggle(EeiSetting.OUTLINE_ENABLED, rightX, y);
        y += ROW_HEIGHT + 4;
        addSlider(EeiSetting.OUTLINE_THICKNESS, 1, leftX, y);
        addHexBox(rightX, y);
        y += ROW_HEIGHT + 4;
        colorRowY = y;
        addPalette(y);
        y += SWATCH_SIZE + 8;

        hologramLabelY = y;
        y += 12;
        addToggle(EeiSetting.HOLOGRAM_ENABLED, leftX, y);
        addSlider(EeiSetting.HOLOGRAM_DISTANCE, 0.5, rightX, y);
        y += ROW_HEIGHT + 8;

        depositLabelY = y;
        y += 12;
        addSlider(EeiSetting.DEPOSIT_RADIUS, 1, leftX, y);
        addToggle(EeiSetting.DEPOSIT_HOTBAR, rightX, y);
        y += ROW_HEIGHT + 4;
        addToggle(EeiSetting.HOTBAR_REFILL, leftX, y);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, Math.max(y + ROW_HEIGHT + 4, height - 26), 200, ROW_HEIGHT)
                .build());
    }

    private static Component label(EeiSetting setting) {
        return Component.translatable("settings.everyeasyinventorymod." + setting.key);
    }

    private static void set(EeiSetting setting, Object value) {
        setting.write(EeiConfig.get(), value);
        ClientState.settingsChanged();
    }

    /** Grise un réglage forcé par le serveur. */
    private <T extends AbstractWidget> T register(EeiSetting setting, T widget) {
        if (ClientState.isForced(setting)) {
            widget.active = false;
            widget.setTooltip(Tooltip.create(Component.translatable("settings.everyeasyinventorymod.forced")));
            locked.add(widget);
        }
        return addRenderableWidget(widget);
    }

    private void addToggle(EeiSetting setting, int x, int y) {
        register(setting, CycleButton.onOffBuilder(ClientState.bool(setting))
                .create(x, y, COLUMN_WIDTH, ROW_HEIGHT, label(setting), (button, value) -> set(setting, value)));
    }

    private void addSlider(EeiSetting setting, double step, int x, int y) {
        register(setting, new SettingSlider(setting, step, x, y));
    }

    private void addHexBox(int x, int y) {
        EditBox box = new EditBox(font, x + 1, y + 1, COLUMN_WIDTH - 2, ROW_HEIGHT - 2, label(EeiSetting.OUTLINE_COLOR));
        box.setMaxLength(7);
        box.setHint(label(EeiSetting.OUTLINE_COLOR));
        box.setValue(EeiSetting.OUTLINE_COLOR.format(ClientState.value(EeiSetting.OUTLINE_COLOR)));
        box.setResponder(text -> {
            Object color = EeiSetting.OUTLINE_COLOR.parse(text);
            box.setTextColor(color == null ? ERROR_COLOR : TEXT_COLOR);
            if (color != null && !ClientState.isForced(EeiSetting.OUTLINE_COLOR)) {
                set(EeiSetting.OUTLINE_COLOR, color);
            }
        });
        box.setEditable(!ClientState.isForced(EeiSetting.OUTLINE_COLOR));
        hexBox = register(EeiSetting.OUTLINE_COLOR, box);
    }

    private void addPalette(int y) {
        for (int i = 0; i < PALETTE.length; i++) {
            int color = PALETTE[i];
            swatches.add(register(EeiSetting.OUTLINE_COLOR, Button.builder(Component.empty(), button -> {
                if (hexBox != null) {
                    hexBox.setValue(EeiSetting.OUTLINE_COLOR.format(color));
                }
            }).bounds(leftX + i * (SWATCH_SIZE + 1), y, SWATCH_SIZE, SWATCH_SIZE).build()));
        }
        locked.removeAll(swatches); // le cadenas du champ de couleur suffit
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("settings.everyeasyinventorymod.section.search"), leftX, searchLabelY, LABEL_COLOR);
        graphics.text(font, Component.translatable("settings.everyeasyinventorymod.section.hologram"), leftX, hologramLabelY, LABEL_COLOR);
        graphics.text(font, Component.translatable("settings.everyeasyinventorymod.section.deposit"), leftX, depositLabelY, LABEL_COLOR);

        int current = (Integer) ClientState.value(EeiSetting.OUTLINE_COLOR);
        for (int i = 0; i < swatches.size(); i++) {
            Button swatch = swatches.get(i);
            graphics.fill(swatch.getX() + 3, swatch.getY() + 3, swatch.getX() + SWATCH_SIZE - 3, swatch.getY() + SWATCH_SIZE - 3, 0xFF000000 | PALETTE[i]);
            if (PALETTE[i] == current) {
                graphics.outline(swatch.getX(), swatch.getY(), SWATCH_SIZE, SWATCH_SIZE, 0xFFFFFFFF);
            }
        }
        // Aperçu de la couleur du contour.
        graphics.fill(rightX + 1, colorRowY + 1, rightX + COLUMN_WIDTH - 1, colorRowY + SWATCH_SIZE - 1, 0xFF000000);
        graphics.fill(rightX + 4, colorRowY + 4, rightX + COLUMN_WIDTH - 4, colorRowY + SWATCH_SIZE - 4, 0xFF000000 | current);
        for (AbstractWidget widget : locked) {
            ScreenTweaks.drawPadlock(graphics, widget.getX() + widget.getWidth() - 7, widget.getY() + 2);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    /** Curseur d'un réglage numérique, par pas de {@code step}. */
    private static final class SettingSlider extends AbstractSliderButton {

        private final EeiSetting setting;
        private final double step;

        SettingSlider(EeiSetting setting, double step, int x, int y) {
            super(x, y, COLUMN_WIDTH, ROW_HEIGHT, Component.empty(), toSlider(setting, number(ClientState.value(setting))));
            this.setting = setting;
            this.step = step;
            updateMessage();
        }

        private static double number(Object value) {
            return ((Number) value).doubleValue();
        }

        private static double toSlider(EeiSetting setting, double value) {
            return (value - setting.min) / (setting.max - setting.min);
        }

        private double current() {
            double raw = Mth.lerp(value, setting.min, setting.max);
            return Mth.clamp(Math.round(raw / step) * step, setting.min, setting.max);
        }

        @Override
        protected void updateMessage() {
            String text = setting.kind == EeiSetting.Kind.INT ? String.valueOf((int) current()) : String.valueOf(current());
            setMessage(label(setting).copy().append(": ").append(text));
        }

        @Override
        protected void applyValue() {
            set(setting, setting.kind == EeiSetting.Kind.INT ? (Object) (int) current() : (Object) current());
        }
    }
}

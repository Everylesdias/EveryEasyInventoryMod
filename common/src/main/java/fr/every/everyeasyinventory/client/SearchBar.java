package fr.every.everyeasyinventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Barre de recherche d'item sous un écran d'inventaire ou de conteneur, avec autocomplétion.
 * Le texte et l'item choisi vivent dans {@link ClientState} : ils suivent le joueur d'un écran à l'autre.
 */
public final class SearchBar {

    public static final int HEIGHT = 12;
    private static final int MAX_SUGGESTIONS = 6;
    private static final int ROW_HEIGHT = 18;
    private static final int PANEL_COLOR = 0xFF101010;
    private static final int PANEL_BORDER = 0xFF606060;
    private static final int ROW_SELECTED = 0xFF3C3C3C;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /** Nom affiché de chaque item, en minuscules pour la comparaison, et son identifiant. */
    private record Entry(Item item, String name, String lower, String id) {
    }

    private static @Nullable List<Entry> entries;
    private static @Nullable String entriesLanguage;

    private final Screen screen;
    private final EditBox box;
    private List<Entry> suggestions = List.of();
    private int selected;
    private boolean picking;

    public SearchBar(Screen screen) {
        this.screen = screen;
        Font font = Minecraft.getInstance().font;
        this.box = new EditBox(font, 0, 0, 100, HEIGHT, Component.translatable("search.everyeasyinventorymod.narration"));
        box.setMaxLength(50);
        box.setHint(Component.translatable("search.everyeasyinventorymod.hint"));
        box.setValue(ClientState.searchText());
        box.setResponder(this::onTextChanged);
    }

    public EditBox box() {
        return box;
    }

    /** Sous l'image de l'écran, ou au-dessus s'il n'y a pas la place en bas. */
    public void layout(int leftPos, int topPos, int imageWidth, int imageHeight) {
        int y = topPos + imageHeight + 2;
        if (y + HEIGHT > screen.height) {
            y = Math.max(0, topPos - HEIGHT - 2);
        }
        box.setRectangle(imageWidth, HEIGHT, leftPos, y);
    }

    // ---------------------------------------------------------------- texte et choix

    private void onTextChanged(String text) {
        if (picking) {
            return;
        }
        if (text.isBlank()) {
            ClientState.setSearch("", null);
            suggestions = List.of();
            return;
        }
        Item current = ClientState.searchItem();
        boolean stillCurrent = current != null && text.equals(displayName(current));
        ClientState.setSearch(text, stillCurrent ? current : null);
        suggestions = stillCurrent ? List.of() : suggest(text);
        selected = 0;
    }

    private void pick(Entry entry) {
        picking = true;
        box.setValue(entry.name());
        picking = false;
        ClientState.setSearch(entry.name(), entry.item());
        suggestions = List.of();
        unfocus();
    }

    private void unfocus() {
        box.setFocused(false);
        if (screen.getFocused() == box) {
            screen.setFocused(null);
        }
    }

    private boolean showsSuggestions() {
        return box.isFocused() && !suggestions.isEmpty();
    }

    private static List<Entry> suggest(String text) {
        String query = text.toLowerCase(Locale.ROOT).trim();
        String idQuery = query.replace(' ', '_');
        Minecraft minecraft = Minecraft.getInstance();
        FeatureFlagSet features = minecraft.level != null ? minecraft.level.enabledFeatures() : null;
        List<Entry> matches = new ArrayList<>();
        for (Entry entry : entries()) {
            if ((features == null || entry.item().isEnabled(features)) && score(entry, query, idQuery) < 4) {
                matches.add(entry);
            }
        }
        matches.sort(Comparator.<Entry>comparingInt(entry -> score(entry, query, idQuery))
                .thenComparingInt(entry -> entry.name().length())
                .thenComparing(Entry::name));
        return matches.subList(0, Math.min(MAX_SUGGESTIONS, matches.size()));
    }

    /** 0 : le nom commence par la recherche ; 1 : un mot du nom ; 2 : n'importe où ; 3 : l'identifiant ; 4 : rien. */
    private static int score(Entry entry, String query, String idQuery) {
        if (entry.lower().startsWith(query)) {
            return 0;
        }
        if (entry.lower().contains(" " + query)) {
            return 1;
        }
        if (entry.lower().contains(query)) {
            return 2;
        }
        return entry.id().contains(idQuery) ? 3 : 4;
    }

    private static List<Entry> entries() {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        if (entries == null || !language.equals(entriesLanguage)) {
            List<Entry> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR) {
                    String name = displayName(item);
                    list.add(new Entry(item, name, name.toLowerCase(Locale.ROOT), BuiltInRegistries.ITEM.getKey(item).getPath()));
                }
            }
            entries = list;
            entriesLanguage = language;
        }
        return entries;
    }

    private static String displayName(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    // ---------------------------------------------------------------- entrées

    /** @return vrai si la touche est consommée par la barre (tout sauf Échap tant qu'elle a le focus) */
    public boolean keyPressed(KeyEvent event) {
        if (!box.isFocused()) {
            return false;
        }
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            return false;
        }
        if (showsSuggestions()) {
            if (key == InputConstants.KEY_DOWN) {
                selected = (selected + 1) % suggestions.size();
                return true;
            }
            if (key == InputConstants.KEY_UP) {
                selected = (selected + suggestions.size() - 1) % suggestions.size();
                return true;
            }
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_TAB) {
                pick(suggestions.get(selected));
                return true;
            }
        } else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            unfocus();
            return true;
        }
        box.keyPressed(event);
        return true;
    }

    /** @return vrai si le clic a choisi une suggestion */
    public boolean mouseClicked(MouseButtonEvent event) {
        if (showsSuggestions()) {
            int[] panel = panel();
            int row = (int) Math.floor((event.y() - panel[1] - 1) / ROW_HEIGHT);
            if (event.x() >= panel[0] && event.x() < panel[0] + panel[2] && row >= 0 && row < suggestions.size()
                    && event.y() >= panel[1] + 1) {
                pick(suggestions.get(row));
                return true;
            }
        }
        if (box.isFocused() && !box.isMouseOver(event.x(), event.y())) {
            unfocus();
        }
        return false;
    }

    // ---------------------------------------------------------------- dessin

    /** x, y, largeur, hauteur du panneau de suggestions : sous la barre, ou au-dessus s'il n'y a pas la place. */
    private int[] panel() {
        int height = suggestions.size() * ROW_HEIGHT + 2;
        int y = box.getY() + box.getHeight() + 1;
        if (y + height > screen.height) {
            y = box.getY() - height - 1;
        }
        return new int[]{box.getX(), y, box.getWidth(), height};
    }

    public void extractSuggestions(GuiGraphicsExtractor graphics) {
        if (!showsSuggestions()) {
            return;
        }
        graphics.nextStratum();
        Font font = Minecraft.getInstance().font;
        int[] panel = panel();
        graphics.fill(panel[0], panel[1], panel[0] + panel[2], panel[1] + panel[3], PANEL_COLOR);
        graphics.outline(panel[0], panel[1], panel[2], panel[3], PANEL_BORDER);
        for (int i = 0; i < suggestions.size(); i++) {
            Entry entry = suggestions.get(i);
            int rowY = panel[1] + 1 + i * ROW_HEIGHT;
            if (i == selected) {
                graphics.fill(panel[0] + 1, rowY, panel[0] + panel[2] - 1, rowY + ROW_HEIGHT, ROW_SELECTED);
            }
            graphics.fakeItem(new ItemStack(entry.item()), panel[0] + 2, rowY + 1);
            String name = font.plainSubstrByWidth(entry.name(), panel[2] - 24);
            graphics.text(font, name, panel[0] + 21, rowY + 5, TEXT_COLOR);
        }
    }
}

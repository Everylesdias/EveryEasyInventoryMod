package fr.every.everyeasyinventory.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Petit bouton avec une icône en pixels à la place du texte : flèche et coffre.
 * Envoyer : la flèche entre dans le coffre. Remplir : la flèche sort du coffre.
 */
public final class IconButton extends Button.Plain {

    public static final int WIDTH = 17;
    public static final int HEIGHT = 12;

    private static final int WOOD = 0xFFC08A3E;
    private static final int OUTLINE = 0xFF3B2410;
    private static final int LATCH = 0xFFE0E0E0;
    private static final int SEND_COLOR = 0xFF6CFF6C;
    private static final int FILL_COLOR = 0xFF6CD4FF;
    private static final int DISABLED = 0xFF808080;

    private static final String[] CHEST = {
            "BBBBBBB",
            "BWWWWWB",
            "BBBLBBB",
            "BWWLWWB",
            "BWWWWWB",
            "BBBBBBB",
    };
    private static final String[] ARROW = {
            "..#..",
            "..##.",
            "#####",
            "..##.",
            "..#..",
    };

    /** Flèche à gauche du coffre (qui y entre) ou à droite (qui en sort). */
    public record Icon(boolean arrowFirst, int color) {
    }

    public static final Icon SEND = new Icon(true, SEND_COLOR);
    public static final Icon FILL = new Icon(false, FILL_COLOR);

    private final Icon icon;

    public IconButton(Component message, Button.OnPress onPress, Icon icon) {
        super(0, 0, WIDTH, HEIGHT, message, onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractDefaultSprite(graphics);
        int x = getX() + 2;
        int y = getY() + 3;
        int arrowColor = active ? icon.color() : DISABLED;
        if (icon.arrowFirst()) {
            drawArrow(graphics, x, y, arrowColor);
            drawChest(graphics, x + 6, y);
        } else {
            drawChest(graphics, x, y);
            drawArrow(graphics, x + 8, y, arrowColor);
        }
    }

    private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y, int color) {
        for (int row = 0; row < ARROW.length; row++) {
            for (int col = 0; col < ARROW[row].length(); col++) {
                if (ARROW[row].charAt(col) == '#') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    private static void drawChest(GuiGraphicsExtractor graphics, int x, int y) {
        for (int row = 0; row < CHEST.length; row++) {
            for (int col = 0; col < CHEST[row].length(); col++) {
                int color = switch (CHEST[row].charAt(col)) {
                    case 'W' -> WOOD;
                    case 'L' -> LATCH;
                    default -> OUTLINE;
                };
                graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
            }
        }
    }
}

package fr.every.everyeasyinventory.config;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Réglages du mod que le joueur peut changer et que le serveur peut forcer.
 * La clé est le nom du champ dans le fichier de configuration et dans la commande {@code /eei}.
 */
public enum EeiSetting {
    SEARCH_ENABLED("searchEnabled", Kind.BOOL, 0, 0, c -> c.searchEnabled, (c, v) -> c.searchEnabled = (Boolean) v),
    OUTLINE_ENABLED("outlineEnabled", Kind.BOOL, 0, 0, c -> c.outlineEnabled, (c, v) -> c.outlineEnabled = (Boolean) v),
    OUTLINE_COLOR("outlineColor", Kind.COLOR, 0, 0xFFFFFF, c -> c.outlineColor, (c, v) -> c.outlineColor = (Integer) v),
    OUTLINE_THICKNESS("outlineThickness", Kind.INT, 1, 5, c -> c.outlineThickness, (c, v) -> c.outlineThickness = (Integer) v),
    HOLOGRAM_ENABLED("hologramEnabled", Kind.BOOL, 0, 0, c -> c.hologramEnabled, (c, v) -> c.hologramEnabled = (Boolean) v),
    HOLOGRAM_DISTANCE("hologramMaxDistance", Kind.DOUBLE, 1, 6, c -> c.hologramMaxDistance, (c, v) -> c.hologramMaxDistance = (Double) v),
    DEPOSIT_RADIUS("depositRadius", Kind.INT, 1, 16, c -> c.depositRadius, (c, v) -> c.depositRadius = (Integer) v),
    DEPOSIT_HOTBAR("depositIncludesHotbar", Kind.BOOL, 0, 0, c -> c.depositIncludesHotbar, (c, v) -> c.depositIncludesHotbar = (Boolean) v),
    HOTBAR_REFILL("hotbarRefill", Kind.BOOL, 0, 0, c -> c.hotbarRefill, (c, v) -> c.hotbarRefill = (Boolean) v);

    public enum Kind {
        BOOL, INT, DOUBLE, COLOR
    }

    private static final EeiConfig DEFAULTS = new EeiConfig();

    public final String key;
    public final Kind kind;
    public final int min;
    public final int max;
    private final Function<EeiConfig, Object> reader;
    private final BiConsumer<EeiConfig, Object> writer;

    EeiSetting(String key, Kind kind, int min, int max, Function<EeiConfig, Object> reader, BiConsumer<EeiConfig, Object> writer) {
        this.key = key;
        this.kind = kind;
        this.min = min;
        this.max = max;
        this.reader = reader;
        this.writer = writer;
    }

    public static @Nullable EeiSetting byKey(String key) {
        for (EeiSetting setting : values()) {
            if (setting.key.equalsIgnoreCase(key)) {
                return setting;
            }
        }
        return null;
    }

    public Object read(EeiConfig config) {
        return reader.apply(config);
    }

    public void write(EeiConfig config, Object value) {
        writer.accept(config, value);
    }

    public Object defaultValue() {
        return read(DEFAULTS);
    }

    /** Valeur lue depuis un texte (fichier serveur, commande, paquet), ou {@code null} si elle n'est pas valide. */
    public @Nullable Object parse(String text) {
        String value = text.trim();
        try {
            return switch (kind) {
                case BOOL -> value.equalsIgnoreCase("true") ? Boolean.TRUE : value.equalsIgnoreCase("false") ? Boolean.FALSE : null;
                case INT -> {
                    int number = Integer.parseInt(value);
                    yield number >= min && number <= max ? number : null;
                }
                case DOUBLE -> {
                    double number = Double.parseDouble(value);
                    yield number >= min && number <= max ? number : null;
                }
                case COLOR -> {
                    String hex = value.startsWith("#") ? value.substring(1) : value;
                    yield hex.length() == 6 ? Integer.parseInt(hex, 16) : null;
                }
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String format(Object value) {
        return kind == Kind.COLOR ? String.format(Locale.ROOT, "#%06X", (Integer) value) : String.valueOf(value);
    }
}

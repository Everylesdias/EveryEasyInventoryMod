package fr.every.everyeasyinventory.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.every.everyeasyinventory.Constants;
import fr.every.everyeasyinventory.config.EeiSetting;
import fr.every.everyeasyinventory.network.Payloads;
import fr.every.everyeasyinventory.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Réglages forcés par le serveur, dans {@code config/everyeasyinventorymod-server.json} ou avec {@code /eei}.
 * Une valeur forcée remplace celle du joueur ; les réglages absents du fichier restent libres.
 */
public final class ServerSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<EeiSetting, Object> forced;

    /** Forme du fichier : {@code {"forced": {"searchEnabled": "false", "outlineColor": "#FF0000"}}}. */
    private static final class FileContent {
        Map<String, String> forced = new TreeMap<>();
    }

    private ServerSettings() {
    }

    private static Path path() {
        return Services.PLATFORM.getConfigDir().resolve(Constants.MOD_ID + "-server.json");
    }

    private static Map<EeiSetting, Object> forced() {
        if (forced == null) {
            reload();
        }
        return forced;
    }

    /** Relit le fichier ; les entrées inconnues ou invalides sont signalées dans le journal et ignorées. */
    public static void reload() {
        Map<EeiSetting, Object> loaded = new EnumMap<>(EeiSetting.class);
        Path path = path();
        if (Files.exists(path)) {
            try {
                FileContent content = GSON.fromJson(Files.readString(path), FileContent.class);
                if (content != null && content.forced != null) {
                    content.forced.forEach((key, text) -> {
                        EeiSetting setting = EeiSetting.byKey(key);
                        Object value = setting == null ? null : setting.parse(text);
                        if (value == null) {
                            Constants.LOG.warn("Réglage forcé ignoré : {} = {}", key, text);
                        } else {
                            loaded.put(setting, value);
                        }
                    });
                }
            } catch (IOException | RuntimeException e) {
                Constants.LOG.warn("Réglages du serveur illisibles, aucun réglage forcé", e);
            }
        }
        forced = loaded;
    }

    private static void save() {
        FileContent content = new FileContent();
        forced().forEach((setting, value) -> content.forced.put(setting.key, setting.format(value)));
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(content));
        } catch (IOException e) {
            Constants.LOG.warn("Impossible d'enregistrer les réglages du serveur", e);
        }
    }

    public static Map<EeiSetting, Object> all() {
        return forced();
    }

    public static void force(MinecraftServer server, EeiSetting setting, Object value) {
        forced().put(setting, value);
        save();
        broadcast(server);
    }

    public static void free(MinecraftServer server, EeiSetting setting) {
        forced().remove(setting);
        save();
        broadcast(server);
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }

    public static void sendTo(ServerPlayer player) {
        Map<String, String> values = new HashMap<>();
        forced().forEach((setting, value) -> values.put(setting.key, setting.format(value)));
        Services.PLATFORM.sendToPlayer(player, new Payloads.ForcedSettings(values));
    }

    /** Valeur qui s'applique à ce joueur : forcée par le serveur, sinon celle de son client, sinon celle par défaut. */
    public static Object value(ServerPlayer player, EeiSetting setting) {
        Object value = forced().get(setting);
        if (value == null) {
            value = PlayerState.of(player).clientSettings.get(setting);
        }
        return value != null ? value : setting.defaultValue();
    }

    public static boolean bool(ServerPlayer player, EeiSetting setting) {
        return (Boolean) value(player, setting);
    }

    public static int intValue(ServerPlayer player, EeiSetting setting) {
        return (Integer) value(player, setting);
    }

    public static double doubleValue(ServerPlayer player, EeiSetting setting) {
        return (Double) value(player, setting);
    }
}

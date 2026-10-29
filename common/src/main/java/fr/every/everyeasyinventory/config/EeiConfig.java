package fr.every.everyeasyinventory.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.every.everyeasyinventory.Constants;
import fr.every.everyeasyinventory.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration du mod, dans config/everyeasyinventorymod.json.
 * Les réglages « serveur » ne servent que là où tourne le monde (le serveur intégré en solo).
 */
public final class EeiConfig {

    // --- Serveur ---
    /** Demi-côté du cube du rangement express : 5 donne un cube de 11 × 11 × 11 centré sur le joueur. */
    public int depositRadius = 5;
    /** Le rangement express vide-t-il aussi la hotbar ? */
    public boolean depositIncludesHotbar = false;
    /** Distance maximale (en blocs, depuis les yeux) pour l'hologramme. */
    public double hologramMaxDistance = 3.0;
    public boolean hotbarRefill = true;

    // --- Client ---
    public boolean hologramEnabled = true;
    /** Cases verrouillées de l'inventaire du joueur : bit i = case i (0-8 hotbar, 9-35 inventaire principal). */
    public long lockedSlots = 0L;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EeiConfig instance;

    public static EeiConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path path() {
        return Services.PLATFORM.getConfigDir().resolve(Constants.MOD_ID + ".json");
    }

    private static EeiConfig load() {
        Path path = path();
        if (Files.exists(path)) {
            try {
                EeiConfig config = GSON.fromJson(Files.readString(path), EeiConfig.class);
                if (config != null) {
                    config.depositRadius = Math.clamp(config.depositRadius, 1, 16);
                    return config;
                }
            } catch (IOException | RuntimeException e) {
                Constants.LOG.warn("Configuration illisible, valeurs par défaut utilisées", e);
            }
        }
        EeiConfig config = new EeiConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException e) {
            Constants.LOG.warn("Impossible d'enregistrer la configuration", e);
        }
    }
}

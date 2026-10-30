package fr.every.everyeasyinventory;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fr.every.everyeasyinventory.client.EeiSettingsScreen;

/**
 * Bouton de réglages dans la liste de Mod Menu. Chargé seulement si Mod Menu est installé.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EeiSettingsScreen::new;
    }
}

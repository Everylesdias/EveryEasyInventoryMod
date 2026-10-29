package fr.every.everyeasyinventory.platform;

import fr.every.everyeasyinventory.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private Services() {
    }

    private static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucune implémentation pour " + clazz.getName()));
    }
}

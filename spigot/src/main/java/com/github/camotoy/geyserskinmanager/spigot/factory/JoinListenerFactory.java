package com.github.camotoy.geyserskinmanager.spigot.factory;

import com.github.camotoy.geyserskinmanager.spigot.GeyserSkinManager;
import com.github.camotoy.geyserskinmanager.spigot.listener.PaperEventListener;
import com.github.camotoy.geyserskinmanager.spigot.listener.SpigotEventListener;
import com.github.camotoy.geyserskinmanager.spigot.listener.SpigotPlatformEventListener;

public final class JoinListenerFactory {
    private JoinListenerFactory() {
    }

    public static SpigotPlatformEventListener create(GeyserSkinManager plugin, boolean showSkins) {
        if (PlatformCapabilityDetector.hasPaperProfileApi()) {
            return new PaperEventListener(plugin, showSkins);
        }

        return new SpigotEventListener(plugin, showSkins);
    }
}
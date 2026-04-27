package com.github.camotoy.geyserskinmanager.spigot.factory;

import com.github.camotoy.geyserskinmanager.spigot.profile.GameProfileWrapper;
import com.github.camotoy.geyserskinmanager.spigot.profile.MinecraftProfileWrapper;
import com.github.camotoy.geyserskinmanager.spigot.profile.PaperProfileWrapper;
import org.bukkit.entity.Player;

public final class ProfileWrapperFactory {
    private ProfileWrapperFactory() {
    }

    public static MinecraftProfileWrapper from(Player player) {
        if (PlatformCapabilityDetector.hasPaperProfileApi()) {
            try {
                return PaperProfileWrapper.from(player);
            } catch (LinkageError | RuntimeException ignored) {
                // Fall back to GameProfile access when the Paper API exists but is not usable.
            }
        }

        return GameProfileWrapper.from(player);
    }

    public static boolean supportsPaperProfileApi() {
        return PlatformCapabilityDetector.hasPaperProfileApi();
    }
}
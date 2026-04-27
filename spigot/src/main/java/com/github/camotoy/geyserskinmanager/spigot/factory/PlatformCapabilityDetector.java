package com.github.camotoy.geyserskinmanager.spigot.factory;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class PlatformCapabilityDetector {
    private static final boolean PAPER_PROFILE_API = detectPaperProfileApi();

    private PlatformCapabilityDetector() {
    }

    public static boolean hasPaperProfileApi() {
        return PAPER_PROFILE_API;
    }

    private static boolean detectPaperProfileApi() {
        try {
            Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            Method getPlayerProfile = Player.class.getMethod("getPlayerProfile");
            Method setPlayerProfile = Player.class.getMethod("setPlayerProfile", profileClass);
            return profileClass.isAssignableFrom(getPlayerProfile.getReturnType()) && setPlayerProfile.getReturnType() == void.class;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }
}
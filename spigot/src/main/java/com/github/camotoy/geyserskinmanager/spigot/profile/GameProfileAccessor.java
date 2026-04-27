package com.github.camotoy.geyserskinmanager.spigot.profile;

import com.mojang.authlib.GameProfile;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class GameProfileAccessor {
    private static final Map<Class<?>, Method> GET_PROFILE_METHODS = new ConcurrentHashMap<>();

    private GameProfileAccessor() {
    }

    static GameProfile getGameProfile(Player player) {
        try {
            Method getProfileMethod = GET_PROFILE_METHODS.computeIfAbsent(player.getClass(), GameProfileAccessor::resolveGetProfileMethod);
            return (GameProfile) getProfileMethod.invoke(player);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException("Could not find GameProfile for " + player.getName(), exception);
        }
    }

    private static Method resolveGetProfileMethod(Class<?> playerClass) {
        try {
            return playerClass.getMethod("getProfile");
        } catch (NoSuchMethodException exception) {
            throw new RuntimeException("getProfile method not found for " + playerClass.getName() + '!', exception);
        }
    }
}
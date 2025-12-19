package com.github.camotoy.geyserskinmanager.common.skinretriever;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.camotoy.geyserskinmanager.common.RawCape;
import com.github.camotoy.geyserskinmanager.common.RawSkin;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.BedrockClientData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class GeyserSkinRetriever implements BedrockSkinRetriever {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public RawCape getBedrockCape(UUID uuid) {
        GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
        if (session == null) {
            return null;
        }

        byte[] capeData = getBedrockData(session.getClientData(), "getCapeData");
        if (session.getClientData().getCapeImageWidth() == 0 || session.getClientData().getCapeImageHeight() == 0 ||
                capeData == null || capeData.length == 0) {
            return null;
        }
        return new RawCape(session.getClientData().getCapeImageWidth(), session.getClientData().getCapeImageHeight(),
                session.getClientData().getCapeId(), capeData);
    }

    @Override
    public RawSkin getBedrockSkin(String name) {
        GeyserSession session = null;
        for (GeyserSession otherSession : GeyserImpl.getInstance().getSessionManager().getSessions().values()) {
            if (name.equals(otherSession.name())) {
                session = otherSession;
                break;
            }
        }
        if (session == null) {
            return null;
        }

        return getImage(session.getClientData());
    }

    @Override
    public RawSkin getBedrockSkin(UUID uuid) {
        GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
        if (session == null) {
            return null;
        }

        return getImage(session.getClientData());
    }

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        return GeyserImpl.getInstance().connectionByUuid(uuid) != null;
    }

    /**
     * Taken from
     * https://github.com/NukkitX/Nukkit/blob/master/src/main/java/cn/nukkit/network/protocol/LoginPacket.java
     */
    private RawSkin getImage(BedrockClientData clientData) {
        byte[] image = getBedrockData(clientData, "getSkinData");
        if (image == null || image.length > (128 * 128 * 4) || clientData.isPersonaSkin()) {
            // System.out.println("Persona skins are not yet supported, sorry!");
            return null;
        }

        byte[] geometryNameBytes = getBedrockData(clientData, "getGeometryName");
        String geometryName = geometryNameBytes != null ? new String(geometryNameBytes, StandardCharsets.UTF_8) : "";
        boolean alex = isAlex(geometryName);

        byte[] geometryDataBytes = getBedrockData(clientData, "getGeometryData");
        String geometryData = geometryDataBytes != null ? new String(geometryDataBytes, StandardCharsets.UTF_8) : "";

        return new RawSkin(
                clientData.getSkinImageWidth(),
                clientData.getSkinImageHeight(),
                image, alex, geometryName,
                geometryData,
                getBedrockDataString(clientData, "getSkinData"));
    }

    private byte[] getBedrockData(BedrockClientData clientData, String methodName) {
        try {
            Method method = clientData.getClass().getMethod(methodName);
            Object result = method.invoke(clientData);
            if (result instanceof String) {
                return Base64.getDecoder().decode((String) result);
            } else if (result instanceof byte[]) {
                return (byte[]) result;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getBedrockDataString(BedrockClientData clientData, String methodName) {
        try {
            Method method = clientData.getClass().getMethod(methodName);
            Object result = method.invoke(clientData);
            if (result instanceof String) {
                return (String) result;
            } else if (result instanceof byte[]) {
                return Base64.getEncoder().encodeToString((byte[]) result);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isAlex(String geometryName) {
        try {
            String defaultGeometryName = OBJECT_MAPPER.readTree(geometryName).get("geometry").get("default").asText();
            return "geometry.humanoid.customSlim".equals(defaultGeometryName);
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }
}

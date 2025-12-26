package com.github.camotoy.geyserskinmanager.fabric;

import com.github.camotoy.geyserskinmanager.common.Configuration;
import com.github.camotoy.geyserskinmanager.common.Constants;
import com.github.camotoy.geyserskinmanager.common.FloodgateUtil;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class GeyserSkinManager implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("geyserskinmanager-fabric");
    private FabricSkinEventListener listener;

    @Override
    public void onInitializeServer() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("GeyserSkinManager");
        Configuration config = Configuration.create(configDir);
        boolean floodgatePresent = FloodgateUtil.isFloodgatePresent(config, LOGGER::warn);

        this.listener = new FabricSkinEventListener(configDir, !floodgatePresent);
        
        LOGGER.info("GeyserSkinManager-Fabric initialized.");
    }
}

package dev.yuliang.cobblemonvanilladeath;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class CobblemonVanillaDeath implements ModInitializer {
    public static final String MOD_ID = "cobblemon_vanilla_death";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Mixins use require = 0 so a Cobblemon update degrades to Cobblemon's own faint instead of
        // crashing. This makes that miss visible in the log.
        warnIfMissing("com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate", "updatePostDeath", "doDeathDrops");
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            warnIfMissing("com.cobblemon.mod.common.client.entity.PokemonClientDelegate", "onSyncedDataUpdated");
            warnIfMissing("com.cobblemon.mod.common.client.render.models.blockbench.PosableModel", "getAnimation");
        }
    }

    public static void warnIfMissing(String className, String... methods) {
        try {
            Class<?> type = Class.forName(className, false, CobblemonVanillaDeath.class.getClassLoader());
            for (String method : methods) {
                if (Arrays.stream(type.getDeclaredMethods()).noneMatch(m -> m.getName().equals(method))) {
                    LOGGER.warn("{}.{} not found - this Cobblemon version is not supported, vanilla deaths may not apply", className, method);
                }
            }
        } catch (ClassNotFoundException | LinkageError e) {
            LOGGER.warn("{} not found - this Cobblemon version is not supported, vanilla deaths may not apply", className);
        }
    }
}

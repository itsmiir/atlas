package com.miir.atlas;

import com.miir.atlas.world.gen.NamespacedMapImage;
import com.miir.atlas.world.gen.biome.source.AtlasBiomeSource;
import com.miir.atlas.world.gen.chunk.AtlasChunkGenerator;
import com.miir.atlas.world.gen.noise.ImageDensityFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Atlas implements ModInitializer {
    public static final String MOD_ID = "atlas";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final int GEN_RADIUS = 256;
    public static MinecraftServer SERVER;

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
    public static final Object2ObjectArrayMap<String, NamespacedMapImage> MAPS = new Object2ObjectArrayMap<>();

    @Override
    public void onInitialize() {
        Registry.register(Registries.CHUNK_GENERATOR, id("atlas"), AtlasChunkGenerator.CODEC);
        Registry.register(Registries.BIOME_SOURCE, id("atlas"), AtlasBiomeSource.CODEC);
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, id("atlas"), ImageDensityFunction.CODEC);
    }
}

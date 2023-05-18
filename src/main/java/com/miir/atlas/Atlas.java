package com.miir.atlas;

import com.miir.atlas.world.gen.AtlasMapInfo;
import com.miir.atlas.world.gen.AtlasPredicates;
import com.miir.atlas.world.gen.NamespacedMapImage;
import com.miir.atlas.world.gen.biome.source.AtlasBiomeSource;
import com.miir.atlas.world.gen.chunk.AtlasChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class Atlas implements ModInitializer {
    public static final String MOD_ID = "atlas";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final int GEN_RADIUS = 8192;
    public static MinecraftServer SERVER;
    public static final RegistryKey<Registry<AtlasMapInfo>> ATLAS_INFO = RegistryKey.ofRegistry(Atlas.id("worldgen/atlas_map_info"));
    public static HashMap<Identifier, NamespacedMapImage> GRAYSCALE_MAPS = new HashMap<>();
    public static HashMap<Identifier, NamespacedMapImage> COLOR_MAPS = new HashMap<>();
    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
    @Override
    public void onInitialize() {
        ArrayList<RegistryLoader.Entry<?>> list = new ArrayList<>();
        list.add(new RegistryLoader.Entry<>(ATLAS_INFO, AtlasMapInfo.CODEC));
        list.addAll(RegistryLoader.DYNAMIC_REGISTRIES);
        RegistryLoader.DYNAMIC_REGISTRIES = list;
        BuiltinRegistries.REGISTRY_BUILDER.addRegistry(ATLAS_INFO, AtlasMapInfo::bootstrap);

        Registry.register(Registries.CHUNK_GENERATOR, id("atlas"), AtlasChunkGenerator.CODEC);
        Registry.register(Registries.BIOME_SOURCE, id("atlas"), AtlasBiomeSource.CODEC);
        AtlasPredicates.register();
    }
    public static NamespacedMapImage getOrCreateMap(String path, NamespacedMapImage.Type type) {
        if (type == NamespacedMapImage.Type.COLOR) {
            return COLOR_MAPS.computeIfAbsent(new Identifier(path), k -> new NamespacedMapImage(path, NamespacedMapImage.Type.COLOR));
        } else if (type == NamespacedMapImage.Type.GRAYSCALE) {
            return GRAYSCALE_MAPS.computeIfAbsent(new Identifier(path), k -> new NamespacedMapImage(path, NamespacedMapImage.Type.GRAYSCALE));
        } else {
            throw new IllegalArgumentException("tried to create a map with an unknown type!");
        }
    }
    public static NamespacedMapImage getOrCreateMap(Identifier path, NamespacedMapImage.Type type) {
        if (type == NamespacedMapImage.Type.COLOR) {
            return COLOR_MAPS.computeIfAbsent(path, k -> new NamespacedMapImage(path.toString(), NamespacedMapImage.Type.COLOR));
        } else if (type == NamespacedMapImage.Type.GRAYSCALE) {
            return GRAYSCALE_MAPS.computeIfAbsent(path, k -> new NamespacedMapImage(path.toString(), NamespacedMapImage.Type.GRAYSCALE));
        } else {
            throw new IllegalArgumentException("tried to create a map with an unknown type!");
        }
    }
}

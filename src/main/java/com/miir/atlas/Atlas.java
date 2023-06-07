package com.miir.atlas;

import com.miir.atlas.world.gen.*;
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
    public static HashMap<Identifier, PngNamespacedMapImage > COLOR_MAPS = new HashMap<>();
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
        if (path.endsWith(".png")) {
            if (type == PngNamespacedMapImage.Type.COLOR) {
                return COLOR_MAPS.computeIfAbsent(new Identifier(path), k -> new PngNamespacedMapImage(path, type));
            } else if (type == PngNamespacedMapImage.Type.GRAYSCALE) {
                return GRAYSCALE_MAPS.computeIfAbsent(new Identifier(path), k -> new PngNamespacedMapImage(path, type));
            } else {
                throw new IllegalArgumentException("tried to create a map with an unknown type!");
            }
        } else if (path.endsWith(".tiff") || path.endsWith(".tif")) {
            if (type == PngNamespacedMapImage.Type.COLOR) {
                throw new IllegalArgumentException("color tiff maps are currently not supported!");
            } else if (type == PngNamespacedMapImage.Type.GRAYSCALE) {
                return GRAYSCALE_MAPS.computeIfAbsent(new Identifier(path), k -> new TiffNamespacedMapImage(path));
            } else {
                throw new IllegalArgumentException("tried to create a map with an unknown type!");
            }
        } else {
            throw new IllegalArgumentException("tried to create a map from an unknown filetype! allowed file extensions are: .png, .tif, .tiff");
        }
    }
    public static NamespacedMapImage getOrCreateMap(Identifier path, NamespacedMapImage.Type type) {
        return getOrCreateMap(path.toString(), type);
    }
}

package com.miir.atlas.world.gen;

import com.miir.atlas.Atlas;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.RegistryElementCodec;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;

public record AtlasMapInfo(Identifier heightmap, float horizontalScale, float verticalScale, int startingY) {
    public static final RegistryKey<AtlasMapInfo> EMPTY = RegistryKey.of(Atlas.ATLAS_INFO, Atlas.id("empty"));
    public static RegistryEntry<AtlasMapInfo> bootstrap(Registry<AtlasMapInfo> registry) {
        return BuiltinRegistries.add(registry, EMPTY, new AtlasMapInfo(Atlas.id("empty"), -1, -1, Integer.MIN_VALUE));
    }
    public static final Codec<AtlasMapInfo> CODEC = RecordCodecBuilder.create(atlasMapInfoInstance -> atlasMapInfoInstance.group(
            Identifier.CODEC.fieldOf("height_map").forGetter(AtlasMapInfo::heightmap),
            Codec.FLOAT.fieldOf("horizontal_scale").forGetter(AtlasMapInfo::horizontalScale),
            Codec.FLOAT.fieldOf("vertical_scale").forGetter(AtlasMapInfo::verticalScale),
            Codec.INT.fieldOf("starting_y").forGetter(AtlasMapInfo::startingY)
    ).apply(atlasMapInfoInstance, AtlasMapInfo::new));
    public static final Codec<RegistryEntry<AtlasMapInfo>> REGISTRY_CODEC = RegistryElementCodec.of(Atlas.ATLAS_INFO, CODEC);
    }

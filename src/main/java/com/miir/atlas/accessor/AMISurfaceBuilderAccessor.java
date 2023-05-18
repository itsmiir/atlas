package com.miir.atlas.accessor;

import com.miir.atlas.world.gen.AtlasMapInfo;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;

public interface AMISurfaceBuilderAccessor {
    void buildSurface(NoiseConfig noiseConfig, BiomeAccess biomeAccess, Registry<Biome> biomeRegistry, boolean useLegacyRandom, HeightContext heightContext, final Chunk chunk, ChunkNoiseSampler chunkNoiseSampler, MaterialRules.MaterialRule materialRule, RegistryEntry<AtlasMapInfo> ami);

}

package com.miir.atlas.mixin;

import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Function;

@Mixin(MaterialRules.MaterialRuleContext.class)
public interface MaterialRuleContextAccessor {
    @Invoker("<init>")
    static MaterialRules.MaterialRuleContext createMaterialRuleContext(SurfaceBuilder surfaceBuilder, NoiseConfig noiseConfig, Chunk chunk, ChunkNoiseSampler chunkNoiseSampler, Function<BlockPos, RegistryEntry<Biome>> posToBiome, Registry<Biome> registry, HeightContext heightContext) {
        throw new UnsupportedOperationException();
    }

    @Invoker
    void callInitVerticalContext(int stoneDepthAbove, int stoneDepthBelow, int fluidHeight, int blockX, int blockY, int blockZ);

    @Invoker
    void callInitHorizontalContext(int blockX, int blockZ);

}

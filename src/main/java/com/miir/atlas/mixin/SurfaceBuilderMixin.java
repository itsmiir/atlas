package com.miir.atlas.mixin;

import com.miir.atlas.accessor.AMISurfaceBuilderAccessor;
import com.miir.atlas.accessor.MapInfoAccessor;
import com.miir.atlas.world.gen.AtlasMapInfo;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.BlockColumn;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SurfaceBuilder.class)
public abstract class SurfaceBuilderMixin implements AMISurfaceBuilderAccessor {
    @Shadow protected abstract void placeBadlandsPillar(BlockColumn column, int x, int z, int surfaceY, HeightLimitView chunk);

    @Shadow protected abstract boolean isDefaultBlock(BlockState state);

    @Shadow @Final private BlockState defaultState;

    @Shadow protected abstract void placeIceberg(int minY, Biome biome, BlockColumn column, BlockPos.Mutable mutablePos, int x, int z, int surfaceY);

    @Override
    public void buildSurface(NoiseConfig noiseConfig, BiomeAccess biomeAccess, Registry<Biome> biomeRegistry, boolean useLegacyRandom, HeightContext heightContext, final Chunk chunk, ChunkNoiseSampler chunkNoiseSampler, MaterialRules.MaterialRule materialRule, RegistryEntry<AtlasMapInfo> ami) {
        final BlockPos.Mutable mutable = new BlockPos.Mutable();
        final ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getStartX();
        int j = chunkPos.getStartZ();
        BlockColumn blockColumn = new BlockColumn(){

            @Override
            public BlockState getState(int y) {
                return chunk.getBlockState(mutable.setY(y));
            }

            @Override
            public void setState(int y, BlockState state) {
                HeightLimitView heightLimitView = chunk.getHeightLimitView();
                if (y >= heightLimitView.getBottomY() && y < heightLimitView.getTopY()) {
                    chunk.setBlockState(mutable.setY(y), state, false);
                    if (!state.getFluidState().isEmpty()) {
                        chunk.markBlockForPostProcessing(mutable);
                    }
                }
            }

            public String toString() {
                return "ChunkBlockColumn " + chunkPos;
            }
        };
        MaterialRules.MaterialRuleContext materialRuleContext = MaterialRuleContextAccessor.createMaterialRuleContext((((SurfaceBuilder) (Object) this)), noiseConfig, chunk, chunkNoiseSampler, biomeAccess::getBiome, biomeRegistry, heightContext);
        ((MapInfoAccessor)(Object) materialRuleContext).atlas_setAMI(ami);
        MaterialRules.BlockStateRule blockStateRule = materialRule.apply(materialRuleContext);
        BlockPos.Mutable mutable2 = new BlockPos.Mutable();
        for (int k = 0; k < 16; ++k) {
            for (int l = 0; l < 16; ++l) {
                int m = i + k;
                int n = j + l;
                int o = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, k, l) + 1;
                mutable.setX(m).setZ(n);
                RegistryEntry<Biome> registryEntry = biomeAccess.getBiome(mutable2.set(m, useLegacyRandom ? 0 : o, n));
                if (registryEntry.matchesKey(BiomeKeys.ERODED_BADLANDS)) {
                    this.placeBadlandsPillar(blockColumn, m, n, o, chunk);
                }
                int p = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, k, l) + 1;
                ((MaterialRuleContextAccessor)(Object)materialRuleContext).callInitHorizontalContext(m, n);
                int q = 0;
                int r = Integer.MIN_VALUE;
                int s = Integer.MAX_VALUE;
                int t = chunk.getBottomY();
                for (int u = p; u >= t; --u) {
                    BlockState blockState2;
                    int v;
                    BlockState blockState = blockColumn.getState(u);
                    if (blockState.isAir()) {
                        q = 0;
                        r = Integer.MIN_VALUE;
                        continue;
                    }
                    if (!blockState.getFluidState().isEmpty()) {
                        if (r != Integer.MIN_VALUE) continue;
                        r = u + 1;
                        continue;
                    }
                    if (s >= u) {
                        s = DimensionType.field_35479;
                        for (v = u - 1; v >= t - 1; --v) {
                            blockState2 = blockColumn.getState(v);
                            if (this.isDefaultBlock(blockState2)) continue;
                            s = v + 1;
                            break;
                        }
                    }
                    v = u - s + 1;
                    ((MaterialRuleContextAccessor)(Object)materialRuleContext).callInitVerticalContext(++q, v, r, m, u, n);
                    if (blockState != this.defaultState || (blockState2 = blockStateRule.tryApply(m, u, n)) == null) continue;
                    blockColumn.setState(u, blockState2);
                }
                if (!registryEntry.matchesKey(BiomeKeys.FROZEN_OCEAN) && !registryEntry.matchesKey(BiomeKeys.DEEP_FROZEN_OCEAN)) continue;
                this.placeIceberg(materialRuleContext.method_39551(), registryEntry.value(), blockColumn, mutable2, m, n, o);
            }
        }
    }
}

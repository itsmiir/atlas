package com.miir.atlas.world.gen.chunk;

import com.google.common.collect.Sets;
import com.miir.atlas.mixin.NoiseChunkGeneratorAccessor;
import com.miir.atlas.world.gen.NamespacedMapImage;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AtlasChunkGenerator extends NoiseChunkGenerator implements NoiseChunkGeneratorAccessor {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    public AtlasChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource, settings);
    }
    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk2) {
        GenerationShapeConfig generationShapeConfig = ((NoiseChunkGeneratorAccessor) this).getSettings().value().generationShapeConfig().trimHeight(chunk2.getHeightLimitView());
        int i = generationShapeConfig.minimumY();
        int j = MathHelper.floorDiv(i, generationShapeConfig.verticalBlockSize());
        int k = MathHelper.floorDiv(generationShapeConfig.height(), generationShapeConfig.verticalBlockSize());
        if (k <= 0) {
            return CompletableFuture.completedFuture(chunk2);
        }
        int l = chunk2.getSectionIndex(k * generationShapeConfig.verticalBlockSize() - 1 + i);
        int m = chunk2.getSectionIndex(i);
        HashSet<ChunkSection> set = Sets.newHashSet();
        for (int n = l; n >= m; --n) {
            ChunkSection chunkSection = chunk2.getSection(n);
            chunkSection.lock();
            set.add(chunkSection);
        }
        return CompletableFuture.supplyAsync(Util.debugSupplier("wgen_fill_noise", () -> this.populateNoise(blender, structureAccessor, noiseConfig, chunk2, j, k)), Util.getMainWorkerExecutor()).whenCompleteAsync((chunk, throwable) -> {
            for (ChunkSection chunkSection : set) {
                chunkSection.unlock();
            }
        }, executor);
    }

    private Chunk populateNoise(Blender blender, StructureAccessor structureAccessor, NoiseConfig noiseConfig, Chunk chunk2, int minimumCellY, int cellHeight) {
        ChunkNoiseSampler chunkNoiseSampler = chunk2.getOrCreateChunkNoiseSampler(chunk -> this.createChunkNoiseSampler((Chunk)chunk, structureAccessor, blender, noiseConfig));
        GenerationShapeConfig config = ((NoiseChunkGeneratorAccessor) this).getSettings().value().generationShapeConfig();
        Heightmap heightmap = chunk2.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk2.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk2.getPos();
        int i = chunkPos.getStartX();
        int j = chunkPos.getStartZ();
        AquiferSampler aquiferSampler = chunkNoiseSampler.getAquiferSampler();
        chunkNoiseSampler.sampleStartNoise();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int k = config.horizontalBlockSize();
        int l = config.verticalBlockSize();
        int m = 16 / k;
        int n = 16 / k;
        for (int o = 0; o < m; ++o) {
            chunkNoiseSampler.sampleEndNoise(o);
            for (int p = 0; p < n; ++p) {
                ChunkSection chunkSection = chunk2.getSection(chunk2.countVerticalSections() - 1);
                for (int q = cellHeight - 1; q >= 0; --q) {
                    chunkNoiseSampler.sampleNoiseCorners(q, p);
                    for (int r = l - 1; r >= 0; --r) {
                        int s = (minimumCellY + q) * l + r;
                        int t = s & 0xF;
                        int u = chunk2.getSectionIndex(s);
                        if (chunk2.getSectionIndex(chunkSection.getYOffset()) != u) {
                            chunkSection = chunk2.getSection(u);
                        }
                        double d = (double)r / (double)l;
                        chunkNoiseSampler.sampleNoiseY(s, d);
                        for (int v = 0; v < k; ++v) {
                            int w = i + o * k + v;
                            int x = w & 0xF;
                            double e = (double)v / (double)k;
                            chunkNoiseSampler.sampleNoiseX(w, e);
                            for (int y = 0; y < k; ++y) {
                                int z = j + p * k + y;
                                int aa = z & 0xF;
                                double f = (double)y / (double)k;
                                chunkNoiseSampler.sampleNoiseZ(z, f);
                                BlockState blockState = chunkNoiseSampler.sampleBlockState();
                                if (blockState == null) {
                                    blockState = ((NoiseChunkGeneratorAccessor) this).getSettings().value().defaultBlock();
                                }
                                if ((blockState == AIR || SharedConstants.isOutsideGenerationArea(chunk2.getPos()))) continue;
                                if (blockState.getLuminance() != 0 && chunk2 instanceof ProtoChunk) {
                                    mutable.set(w, s, z);
                                    ((ProtoChunk)chunk2).addLightSource(mutable);
                                }
                                chunkSection.setBlockState(x, t, aa, blockState, false);
                                heightmap.trackUpdate(x, s, aa, blockState);
                                heightmap2.trackUpdate(x, s, aa, blockState);
                                if (!aquiferSampler.needsFluidTick() || blockState.getFluidState().isEmpty()) continue;
                                mutable.set(w, s, z);
                                chunk2.markBlockForPostProcessing(mutable);
                            }
                        }
                    }
                }
            }
            chunkNoiseSampler.swapBuffers();
        }
        chunkNoiseSampler.stopInterpolation();
        return chunk2;
    }

}

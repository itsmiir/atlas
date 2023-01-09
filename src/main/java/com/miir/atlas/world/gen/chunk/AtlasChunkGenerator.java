package com.miir.atlas.world.gen.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.NamespacedMapImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class AtlasChunkGenerator extends ChunkGenerator {
    private static final int MIN_GENERATION_HEIGHT = -63;

    private final NamespacedMapImage image;

    private final int seaLevel;
    private final int minimumY;
    private final RegistryEntry<ChunkGeneratorSettings> settings;


    public AtlasChunkGenerator(String path, BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings, int minimumY) {
        super(biomeSource);
        this.seaLevel = settings.value().seaLevel();
        this.minimumY = minimumY;
        this.image = new NamespacedMapImage(path, NamespacedMapImage.Type.HEIGHTMAP);
        this.settings = settings;
    }

    public void findHeightmap(MinecraftServer server) throws IOException {
        this.image.initialize(server);
        Atlas.LOGGER.info("found elevation data for dimension "+this.getPath()+" in a "+this.image.getWidth()+"x"+this.image.getHeight()+" map!");
    }

    public static final Codec<AtlasChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("path").forGetter(AtlasChunkGenerator::getPath),
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(AtlasChunkGenerator::getBiomeSource),
            ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(AtlasChunkGenerator::getSettings),
            Codec.INT.fieldOf("starting_y").forGetter(AtlasChunkGenerator::getMinimumY)
    ).apply(instance, AtlasChunkGenerator::new));

    private int getElevation(int x, int z) {
        x += this.image.getWidth() / 2;
        z += this.image.getHeight() / 2;
        if (x < 0 || z < 0 || x >= this.image.getWidth() || z >= this.image.getHeight()) return -1;
        return this.image.getPixels()[z][x]+minimumY;
    }

    public RegistryEntry<ChunkGeneratorSettings> getSettings() {
        return this.settings;
    }

    private String getPath() {return this.image.getPath();}
    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk2, GenerationStep.Carver carverStep) {
        BiomeAccess biomeAccess2 = biomeAccess.withSource((biomeX, biomeY, biomeZ) -> this.biomeSource.getBiome(biomeX, biomeY, biomeZ, noiseConfig.getMultiNoiseSampler()));
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
        int i = 8;
        ChunkPos chunkPos = chunk2.getPos();
        ChunkNoiseSampler chunkNoiseSampler = chunk2.getOrCreateChunkNoiseSampler(chunk -> this.createChunkNoiseSampler(chunk, structureAccessor, Blender.getBlender(chunkRegion), noiseConfig));
        AquiferSampler aquiferSampler = chunkNoiseSampler.getAquiferSampler();
        CarverContext carverContext = new CarverContext(new NoiseChunkGenerator(this.biomeSource, this.settings)/*lmao*/, chunkRegion.getRegistryManager(), chunk2.getHeightLimitView(), chunkNoiseSampler, noiseConfig, this.settings.value().surfaceRule());
        CarvingMask carvingMask = ((ProtoChunk)chunk2).getOrCreateCarvingMask(carverStep);
        for (int j = -8; j <= 8; ++j) {
            for (int k = -8; k <= 8; ++k) {
                ChunkPos chunkPos2 = new ChunkPos(chunkPos.x + j, chunkPos.z + k);
                Chunk chunk22 = chunkRegion.getChunk(chunkPos2.x, chunkPos2.z);
                GenerationSettings generationSettings = chunk22.getOrCreateGenerationSettings(() -> this.getGenerationSettings(this.biomeSource.getBiome(BiomeCoords.fromBlock(chunkPos2.getStartX()), 0, BiomeCoords.fromBlock(chunkPos2.getStartZ()), noiseConfig.getMultiNoiseSampler())));
                Iterable<RegistryEntry<ConfiguredCarver<?>>> iterable = generationSettings.getCarversForStep(carverStep);
                int l = 0;
                for (RegistryEntry<ConfiguredCarver<?>> registryEntry : iterable) {
                    ConfiguredCarver<?> configuredCarver = registryEntry.value();
                    chunkRandom.setCarverSeed(seed + (long)l, chunkPos2.x, chunkPos2.z);
                    if (configuredCarver.shouldCarve(chunkRandom)) {
                        configuredCarver.carve(carverContext, chunk2, biomeAccess2::getBiome, chunkRandom, aquiferSampler, chunkPos2, carvingMask);
                    }
                    ++l;
                }
            }
        }
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        if (SharedConstants.isOutsideGenerationArea(chunk.getPos())) {
            return;
        }
        HeightContext heightContext = new HeightContext(this, region);
        this.buildSurface(chunk, heightContext, noiseConfig, structures, region.getBiomeAccess(), region.getRegistryManager().get(RegistryKeys.BIOME), Blender.getBlender(region));
    }

    @VisibleForTesting
    public void buildSurface(Chunk chunk, HeightContext heightContext, NoiseConfig noiseConfig, StructureAccessor structureAccessor, BiomeAccess biomeAccess, Registry<Biome> biomeRegistry, Blender blender) {
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler(chunk3 -> this.createChunkNoiseSampler(chunk3, structureAccessor, blender, noiseConfig));
        ChunkGeneratorSettings chunkGeneratorSettings = this.settings.value();
        noiseConfig.getSurfaceBuilder().buildSurface(noiseConfig, biomeAccess, biomeRegistry, chunkGeneratorSettings.usesLegacyRandom(), heightContext, chunk, chunkNoiseSampler, chunkGeneratorSettings.surfaceRule());
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        ChunkPos chunkPos = region.getCenterPos();
        RegistryEntry<Biome> registryEntry = region.getBiome(chunkPos.getStartPos().withY(region.getTopY() - 1));
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
        chunkRandom.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
        SpawnHelper.populateEntities(region, registryEntry, chunkPos, chunkRandom);
    }

    @Override
    public int getWorldHeight() {
        return this.settings.value().generationShapeConfig().height();
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk2) {
        return CompletableFuture.supplyAsync(Util.debugSupplier("wgen_fill_noise", () -> this.populateNoise(chunk2)), Util.getMainWorkerExecutor());
    }
    private Chunk populateNoise(Chunk chunk) {
        int minY = settings.value().generationShapeConfig().minimumY();
        Heightmap oceanHeightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap surfaceHeightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        BlockState defaultBlock = this.settings.value().defaultBlock();
        BlockState defaultFluid = this.settings.value().defaultFluid();
        int offsetX = chunk.getPos().x << 4;
        int offsetZ = chunk.getPos().z << 4;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = 0; x < 16; x++) {
            mutable.setX(x);
            for (int z = 0; z < 16; z++) {
                mutable.setZ(z);
                int elevation = this.getElevation(x+offsetX, z+offsetZ);
                if (elevation != -1 && elevation >= minY) {
                    for (int y = minY; y < elevation; y++) {
                        mutable.setY(y);
                        chunk.setBlockState(mutable, defaultBlock, false);
                    }
                    if (elevation < seaLevel) {
                        for (int y = elevation; y < seaLevel; y++) {
                            mutable.setY(y);
                            chunk.setBlockState(mutable, defaultFluid, false);
                        }
                        surfaceHeightmap.trackUpdate(x, elevation, z, defaultFluid);
                    } else {
                        surfaceHeightmap.trackUpdate(x, elevation, z, defaultBlock);
                    }
                    oceanHeightmap.trackUpdate(x, elevation, z, defaultBlock);
                }
            }
        }
        return chunk;
    }

    @Override
    public int getSeaLevel() {
        return this.seaLevel;
    }

    @Override
    public int getMinimumY() {
        return this.settings.value().generationShapeConfig().minimumY();
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return (
                heightmap == Heightmap.Type.OCEAN_FLOOR_WG || heightmap == Heightmap.Type.OCEAN_FLOOR)
                ? this.getElevation(x, z)
                : Math.max(this.seaLevel, this.getElevation(x, z));
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int elevation = this.getElevation(x, z);
        if (elevation <= 0) return new VerticalBlockSample(0, new BlockState[]{Blocks.AIR.getDefaultState()});
        if (elevation < seaLevel) {
            return new VerticalBlockSample(
                    MIN_GENERATION_HEIGHT,
                    Stream.concat(
                            Stream.generate(() -> this.settings.value().defaultBlock()).limit(elevation),
                            Stream.generate(() -> this.settings.value().defaultFluid()).limit(seaLevel - elevation)
            ).toArray(BlockState[]::new));
        }
            return new VerticalBlockSample(
                    MIN_GENERATION_HEIGHT,
                    Stream.generate(() -> this.settings.value().defaultBlock()).limit(elevation).toArray(BlockState[]::new)

            );
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
    }

    private ChunkNoiseSampler createChunkNoiseSampler(Chunk chunk, StructureAccessor world, Blender blender, NoiseConfig noiseConfig) {
        return ChunkNoiseSampler.create(chunk, noiseConfig, StructureWeightSampler.createStructureWeightSampler(world, chunk.getPos()), this.settings.value(), createFluidLevelSampler(this.settings.value()), blender);
    }
    private static AquiferSampler.FluidLevelSampler createFluidLevelSampler(ChunkGeneratorSettings settings) {
        AquiferSampler.FluidLevel fluidLevel = new AquiferSampler.FluidLevel(-54, Blocks.LAVA.getDefaultState());
        int i = settings.seaLevel();
        AquiferSampler.FluidLevel fluidLevel2 = new AquiferSampler.FluidLevel(i, settings.defaultFluid());
        AquiferSampler.FluidLevel fluidLevel3 = new AquiferSampler.FluidLevel(DimensionType.MIN_HEIGHT * 2, Blocks.AIR.getDefaultState());
        return (x, y, z) -> {
            if (y < Math.min(-54, i)) {
                return fluidLevel;
            }
            return fluidLevel2;
        };
    }
}

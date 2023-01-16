package com.miir.atlas.world.gen.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
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
import net.minecraft.util.math.MathHelper;
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
import net.minecraft.world.chunk.ChunkSection;
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
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class AtlasChunkGenerator extends ChunkGenerator {
    private final NamespacedMapImage heightmap;
    private final NamespacedMapImage aquifer;
    private final int seaLevel;
    private final int minimumY;
    private final RegistryEntry<ChunkGeneratorSettings> settings;
    private final float verticalScale;
    private final float horizontalScale;


    public AtlasChunkGenerator(String path, Optional<String> aquiferPath, BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings, int minimumY, Optional<Float> verticalScale, Optional<Float> horizontalScale) {
        super(biomeSource);
        this.seaLevel = settings.value().seaLevel();
        this.minimumY = minimumY;
        this.verticalScale = verticalScale.orElse(1f);
        if (this.verticalScale != 1) Atlas.LOGGER.warn("using non-default vertical scale for a dimension! this feature is in alpha, expect weird generation!");
        this.horizontalScale = horizontalScale.orElse(1f);
        this.heightmap = new NamespacedMapImage(path, NamespacedMapImage.Type.HEIGHTMAP);
        this.aquifer = aquiferPath.map(s -> new NamespacedMapImage(s, NamespacedMapImage.Type.AQUIFER)).orElse(null);
        this.settings = settings;
    }

    public void findMaps(MinecraftServer server, String levelName) throws IOException {
        this.heightmap.initialize(this.getPath(), server);
        Atlas.LOGGER.info("found elevation data for dimension " + levelName + " in a " + this.heightmap.getWidth() + "x" + this.heightmap.getHeight() + " map: " + getPath());
        try {
            this.aquifer.initialize(this.getAquiferPath().get(), server);
            Atlas.LOGGER.info("found aquifer data for dimension " + levelName + " in a " + this.aquifer.getWidth() + "x" + this.aquifer.getHeight() + " map: " + getAquiferPath());
        } catch (NoSuchElementException e) {
            Atlas.LOGGER.warn("couldn't find aquifer for dimension " + levelName+", defaulting to sea level!");
        }
    }

    public static final Codec<AtlasChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("height_map").forGetter(AtlasChunkGenerator::getPath),
            Codec.STRING.optionalFieldOf("aquifer").forGetter(AtlasChunkGenerator::getAquiferPath),
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(AtlasChunkGenerator::getBiomeSource),
            ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(AtlasChunkGenerator::getSettings),
            Codec.INT.fieldOf("starting_y").forGetter(AtlasChunkGenerator::getMinimumY),
            Codec.FLOAT.optionalFieldOf("vertical_scale").forGetter(AtlasChunkGenerator::getVerticalScale),
            Codec.FLOAT.optionalFieldOf("horizontal_scale").forGetter(AtlasChunkGenerator::getHorizontalScale)
    ).apply(instance, AtlasChunkGenerator::new));

    private Optional<String> getAquiferPath() {
        return this.aquifer == null ? Optional.empty() : Optional.of(this.aquifer.getPath());
    }

    private int getElevation(int x, int z) {
        float xR = (x/horizontalScale);
        float zR = (z/horizontalScale);
        xR += this.heightmap.getWidth()  / 2f; // these will always be even numbers
        zR += this.heightmap.getHeight() / 2f;
        if (xR < 0 || zR < 0 || xR >= this.heightmap.getWidth() || zR >= this.heightmap.getHeight()) return -1;
        int truncatedX = (int)Math.floor(xR);
        int truncatedZ = (int)Math.floor(zR);
        double d = this.lerpElevation(truncatedX, xR-truncatedX, truncatedZ, zR-truncatedZ);
        return (int) Math.round(this.verticalScale*(d)+minimumY);
    }

    public float lerpElevation(int truncatedX, float xR, int truncatedZ, float zR) {
        int dx = 0, dz = 0;
        int u0 = Math.max(0, truncatedX + dx), v0 = Math.max(0, truncatedZ + dz);
        int u1 = Math.min(this.heightmap.getWidth()-1, u0 + 1),    v1 = Math.min(v0 + 1, this.heightmap.getHeight()-1);
        float i00, i01, i10, i11;
//        this.heightmap.loadPixelsInRange(u0, v0, true, Atlas.GEN_RADIUS);
        i00 = this.heightmap.getPixels()[v0][u0];
        i01 = this.heightmap.getPixels()[v1][u0];
        i10 = this.heightmap.getPixels()[v0][u1];
        i11 = this.heightmap.getPixels()[v1][u1];
        return (float) MathHelper.lerp2(Math.abs(xR), Math.abs(zR), i00, i10, i01, i11);
    }
    public float lerpAquifer(int truncatedX, float xR, int truncatedZ, float zR) {
        int u0 = Math.max(0, truncatedX), v0 = Math.max(0, truncatedZ);
        int u1 = Math.min(this.aquifer.getWidth()-1, u0 + 1),    v1 = Math.min(v0 + 1, this.aquifer.getHeight()-1);
        float i00, i01, i10, i11;
//        this.aquifer.loadPixelsInRange(u0, v0, true, Atlas.GEN_RADIUS);
        i00 = this.aquifer.getPixels()[v0][u0];
        i01 = this.aquifer.getPixels()[v1][u0];
        i10 = this.aquifer.getPixels()[v0][u1];
        i11 = this.aquifer.getPixels()[v1][u1];
        return (float) MathHelper.lerp2(Math.abs(xR), Math.abs(zR), i00, i10, i01, i11);
    }

    private int getAquifer(int x, int z) {
        float xR = (x/horizontalScale);
        float zR = (z/horizontalScale);
        xR += this.aquifer.getWidth() / 2f; // these will always be even numbers
        zR += this.aquifer.getHeight() / 2f;
        if (xR < 0 || zR < 0 || xR >= this.aquifer.getWidth() || zR >= this.aquifer.getHeight()) return -1;
        int truncatedX = (int)Math.floor(xR);
        int truncatedZ = (int)Math.floor(zR);
        double d = this.lerpAquifer(truncatedX, xR-truncatedX, truncatedZ, zR-truncatedZ);
        return (int) Math.round(this.verticalScale*(d)+minimumY);
    }

    public Optional<Float> getVerticalScale() {return this.verticalScale == 0 ? Optional.of(1f) : Optional.of(this.verticalScale);}
    public Optional<Float> getHorizontalScale() {return this.horizontalScale == 0 ? Optional.of(1f) : Optional.of(this.horizontalScale);}
    public RegistryEntry<ChunkGeneratorSettings> getSettings() {return this.settings;}

    private String getPath() {return this.heightmap.getPath();}
    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateBiomes(Executor executor, NoiseConfig noiseConfig, Blender blender, StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.supplyAsync(Util.debugSupplier("init_biomes", () -> {
            chunk.populateBiomes(this.biomeSource, noiseConfig.getMultiNoiseSampler());
            return chunk;
        }), Util.getMainWorkerExecutor());
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
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        GenerationShapeConfig generationShapeConfig = this.settings.value().generationShapeConfig().trimHeight(chunk.getHeightLimitView());
        int i = generationShapeConfig.minimumY();
        int k = MathHelper.floorDiv(generationShapeConfig.height(), generationShapeConfig.verticalBlockSize());
        if (k <= 0) {
            return CompletableFuture.completedFuture(chunk);
        }
//        int l = chunk.getSectionIndex(k * generationShapeConfig.verticalBlockSize() - 1 + i);
//        int m = chunk.getSectionIndex(i);
//        HashSet<ChunkSection> set = Sets.newHashSet();
//        for (int n = l; n >= m; --n) {
//            ChunkSection chunkSection = chunk.getSection(n);
//            chunkSection.lock();
//            set.add(chunkSection);
//        }
        int x = chunk.getPos().x << 4;
        int z = chunk.getPos().z << 4;
        float xR = (x/horizontalScale);
        float zR = (z/horizontalScale);
        xR += this.heightmap.getWidth()  / 2f; // these will always be even numbers
        zR += this.heightmap.getHeight() / 2f;
        int truncatedX = (int)Math.floor(xR);
        int truncatedZ = (int)Math.floor(zR);
        if (truncatedX < 0 || truncatedZ < 0 || truncatedX >= this.heightmap.getWidth() || truncatedZ >= this.heightmap.getHeight()) return CompletableFuture.completedFuture(chunk);
        this.heightmap.loadPixelsInRange(truncatedX, truncatedZ, true, Atlas.GEN_RADIUS);
        if (this.aquifer != null) this.aquifer.loadPixelsInRange(truncatedX, truncatedZ, true, Atlas.GEN_RADIUS);

//        return CompletableFuture.supplyAsync(Util.debugSupplier("wgen_fill_noise", () -> this.populateNoise(chunk)), Util.getMainWorkerExecutor()).whenCompleteAsync((chunk2, throwable) -> {
//            for (ChunkSection chunkSection : set) {
//                chunkSection.unlock();
//            }
//        }, executor);
        return CompletableFuture.supplyAsync(Util.debugSupplier("wgen_fill_noise", () -> this.populateNoise(chunk)), Util.getMainWorkerExecutor());
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
                int seaLevel = this.seaLevel;
                if (this.aquifer != null) seaLevel = Math.min(Math.max(getAquifer(x+offsetX, z+offsetZ), seaLevel), this.minimumY+this.getWorldHeight());
                int elevation = Math.min(this.getElevation(x+offsetX, z+offsetZ), this.minimumY+this.getWorldHeight());
                if (elevation != -1 && elevation >= minY) {
                    for (int y = minY; y < elevation; y++) {
                        mutable.setY(y);
                        chunk.setBlockState(mutable, defaultBlock, false);
                    }
                    if (defaultBlock.getLuminance() != 0 && chunk instanceof ProtoChunk) {
                        ((ProtoChunk)chunk).addLightSource(mutable);
                    }
                    if (elevation < seaLevel) {
                        for (int y = elevation; y < seaLevel; y++) {
                            mutable.setY(y);
                            chunk.setBlockState(mutable, defaultFluid, false);
                            chunk.markBlockForPostProcessing(mutable);
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
                    this.settings.value().generationShapeConfig().minimumY(),
                    Stream.concat(
                            Stream.generate(() -> this.settings.value().defaultBlock()).limit(elevation),
                            Stream.generate(() -> this.settings.value().defaultFluid()).limit(seaLevel - elevation)
            ).toArray(BlockState[]::new));
        }
            return new VerticalBlockSample(
                    this.settings.value().generationShapeConfig().minimumY(),
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

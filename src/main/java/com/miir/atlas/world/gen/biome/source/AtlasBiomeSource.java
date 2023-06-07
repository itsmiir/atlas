package com.miir.atlas.world.gen.biome.source;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.AtlasMapInfo;
import com.miir.atlas.world.gen.PngNamespacedMapImage;
import com.miir.atlas.world.gen.biome.BiomeEntry;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class AtlasBiomeSource extends BiomeSource {
    public static final RegistryKey<Biome> EMPTY_BIOME = RegistryKey.of(RegistryKeys.BIOME, Atlas.id("empty"));
    private static final Identifier EMPTY = Atlas.id("_not_impl");
    private final PngNamespacedMapImage image;
    private final List<BiomeEntry> biomeEntries;
    private final RegistryEntry<Biome> defaultBiome;
    private final RegistryEntry<AtlasMapInfo> mapInfo;
    private final Optional<MultiNoiseUtil.Entries<RegistryEntry<Biome>>> caveBiomes;
    private final Int2ObjectArrayMap<RegistryEntry<Biome>> biomeToColor = new Int2ObjectArrayMap<>();
    private final int belowDepth;

//    todo: read the mapInfo from the CG (probably harder to do than the surface rule)
    protected AtlasBiomeSource(String path, List<BiomeEntry> biomeToColor, Optional<MultiNoiseUtil.Entries<RegistryEntry<Biome>>> caveBiomes, Optional<RegistryEntry<Biome>> defaultBiome, RegistryEntry<AtlasMapInfo> mapInfo, int belowDepth) {
        super(Stream.concat(biomeToColor.stream().map(BiomeEntry::getTopBiome), caveBiomes.isPresent() ? (caveBiomes.get().getEntries().stream().map(Pair::getSecond)) : Stream.empty()).toList());
        this.image = (PngNamespacedMapImage) Atlas.getOrCreateMap(path, PngNamespacedMapImage.Type.COLOR);
        this.biomeEntries = biomeToColor;
        this.caveBiomes = caveBiomes;
        this.defaultBiome = defaultBiome.orElse(this.biomeEntries.get(0).getTopBiome());
        this.mapInfo = mapInfo;
        this.belowDepth = belowDepth;
        for (BiomeEntry entry : this.biomeEntries) {
            this.biomeToColor.put(entry.getColor(), entry.getTopBiome());
        }
    }

//    I HATE OOP I HATE OOP I HATE OOP
    public static final Codec<AtlasBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("biome_map").forGetter(AtlasBiomeSource::getPath),
            Codecs.nonEmptyList(BiomeEntry.CODEC.listOf()).fieldOf("biomes").forGetter(AtlasBiomeSource::getBiomeEntries),
            Codecs.nonEmptyList(RecordCodecBuilder.<Pair<MultiNoiseUtil.NoiseHypercube, RegistryEntry<Biome>>>create(instance2 -> instance2.group(
                    MultiNoiseUtil.NoiseHypercube.CODEC.fieldOf("parameters").forGetter(Pair::getFirst),
                    (Biome.REGISTRY_CODEC.fieldOf("biome")).forGetter(Pair::getSecond))
            .apply(instance2, Pair::of))
            .listOf()).xmap(MultiNoiseUtil.Entries::new, MultiNoiseUtil.Entries::getEntries).optionalFieldOf("cave_biomes").forGetter(AtlasBiomeSource::getCaveBiomes),
            Biome.REGISTRY_CODEC.optionalFieldOf("default").forGetter(AtlasBiomeSource::getDefaultBiome),
            AtlasMapInfo.REGISTRY_CODEC.fieldOf("map_info").forGetter(AtlasBiomeSource::getMapInfo),
            Codec.INT.optionalFieldOf("below_depth", Integer.MIN_VALUE).forGetter(AtlasBiomeSource::getBelowDepth)

    ).apply(instance, AtlasBiomeSource::new));

    private RegistryEntry<AtlasMapInfo> getMapInfo() {return this.mapInfo;}
    private int getBelowDepth() {return this.belowDepth;}
    public List<BiomeEntry> getBiomeEntries() {
        return this.biomeEntries;
    }
    public Optional<MultiNoiseUtil.Entries<RegistryEntry<Biome>>> getCaveBiomes() {return this.caveBiomes;}
    public Optional<RegistryEntry<Biome>> getDefaultBiome(){return Optional.of(this.defaultBiome);}
    public String getPath() {
        return this.image.getPath();
    }
    @Override
    protected Codec<AtlasBiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    public Set<RegistryEntry<Biome>> getBiomes() {
        return super.getBiomes();
    }

    public void findBiomeMap(MinecraftServer server, String levelName) throws IOException {
        this.image.initialize(server);
        Atlas.LOGGER.info("found biomes for dimension " + levelName + " in a " + this.image.getWidth() + "x" + this.image.getHeight() + " map: " + getPath());
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, MultiNoiseUtil.MultiNoiseSampler noiseSampler) {
        StringBuilder builder = new StringBuilder("Source: ");
        AtlasMapInfo ami = this.mapInfo.value();
        double elevation = Atlas.getOrCreateMap(ami.heightmap(), PngNamespacedMapImage.Type.GRAYSCALE).getElevation(pos.getX(), pos.getZ(), ami.horizontalScale(), ami.verticalScale(), ami.startingY());
        if (pos.getY() < (elevation - this.belowDepth)) {
            builder.append("Noise");
        } else {
            builder.append("Prescribed");
        }
        info.add(builder.toString());
        info.add("[Atlas] Depth: " + (elevation - pos.getY()));
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        AtlasMapInfo ami = this.mapInfo.value();
        float horizontalScale = ami.horizontalScale();
        Identifier heightmapPath = ami.heightmap();
        // short-circuit with cave biomes
        double elevation = Atlas.getOrCreateMap(heightmapPath, PngNamespacedMapImage.Type.GRAYSCALE).getElevation(x << 2, z << 2, horizontalScale, ami.verticalScale(), ami.startingY());
        if (y<<2 < (elevation - this.belowDepth)) {
            if (!this.mapInfo.value().heightmap().equals(EMPTY) && this.caveBiomes.isPresent()) {
                RegistryEntry<Biome> biome = this.caveBiomes.get().get(noise.sample(x, y, z));
                if (!biome.equals(this.defaultBiome) && (biome.getKey().isEmpty() || !biome.getKey().get().equals(EMPTY_BIOME))) {
                    return biome;
                }
            }
        }
        x <<= 2;
        z <<= 2;
        x = Math.round(x/horizontalScale);
        z = Math.round(z/horizontalScale);
        x += this.image.getWidth() / 2;
        z += this.image.getHeight() / 2;
        if (x < 0 || z < 0 || x >= this.image.getWidth() || z >= this.image.getHeight()) return this.defaultBiome;
//        this.image.loadPixelsInRange(x, z, false, Atlas.GEN_RADIUS);
        int color = (int)this.image.getPixels()[z][x];
        RegistryEntry<Biome> biome = this.biomeToColor.get(color);
        if (biome == null) {
            return this.getClosest(color);
        } else {
             return biome;
        }
    }
    private RegistryEntry<Biome> getClosest(int color) {
        double minDist = -1;
        int closest = -1;
        int r = color & 0xFF0000 >> 16;
        int g = color & 0xFF00 >> 8;
        int b = color & 0xFF;
        for (int c :
                this.biomeToColor.keySet()) {
            double dist = (Math.pow((c & 0xFF0000 >> 16)-r, 2) + Math.pow((c & 0xFF00 >> 8)-g, 2) +Math.pow((c & 0xFF)-b, 2));
            if (dist < minDist || minDist == -1) {
                minDist = dist;
                closest = c;
            }
        }
        return this.biomeToColor.getOrDefault(closest, defaultBiome);
    }
}

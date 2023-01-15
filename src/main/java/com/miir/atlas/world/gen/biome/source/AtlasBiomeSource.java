package com.miir.atlas.world.gen.biome.source;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.NamespacedMapImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.io.IOException;
import java.util.List;

public class AtlasBiomeSource extends BiomeSource {
    private final NamespacedMapImage image;
    private final List<BiomeEntry> biomeEntries;
    private final RegistryEntry<Biome> defaultBiome;
    private final Int2ObjectArrayMap<RegistryEntry<Biome>> biomes = new Int2ObjectArrayMap<>();
    private final float horizontalScale;

    protected AtlasBiomeSource(String path, List<BiomeEntry> biomes, RegistryEntry<Biome> defaultBiome, float horizontalScale) {
        super(biomes.stream().map(biomeEntry -> biomeEntry.biome).toList());
        this.image = new NamespacedMapImage(path, NamespacedMapImage.Type.BIOMES);
        this.biomeEntries = biomes;
        this.defaultBiome = defaultBiome;
        this.horizontalScale = horizontalScale;
        for (BiomeEntry entry : this.biomeEntries) this.biomes.put(entry.color, entry.biome);
    }

    public static final Codec<AtlasBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("biome_map").forGetter(AtlasBiomeSource::getPath),
            Codecs.nonEmptyList(BiomeEntry.CODEC.listOf()).fieldOf("biomes").forGetter(AtlasBiomeSource::getBiomeEntries),
            Biome.REGISTRY_CODEC.fieldOf("default").forGetter(AtlasBiomeSource::getDefaultBiome),
            Codec.FLOAT.fieldOf("horizontal_scale").forGetter(AtlasBiomeSource::getHorizontalScale)
    ).apply(instance, AtlasBiomeSource::new));

    public List<BiomeEntry> getBiomeEntries() {
        return this.biomeEntries;
    }
    public RegistryEntry<Biome> getDefaultBiome(){return this.defaultBiome;}
    public String getPath() {
        return this.image.getPath();
    }
    public float getHorizontalScale() {return this.horizontalScale;}

    @Override
    protected Codec<AtlasBiomeSource> getCodec() {
        return CODEC;
    }

    public void findBiomeMap(MinecraftServer server, String levelName) throws IOException {
        this.image.initialize(getPath(), server);
        Atlas.LOGGER.info("found biomes for dimension " + levelName + " in a " + this.image.getWidth() + "x" + this.image.getHeight() + " map: " + getPath());
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        x *=4;
        z *=4;
        x = Math.round(x/horizontalScale);
        z = Math.round(z/horizontalScale);
        x += this.image.getWidth() / 2;
        z += this.image.getHeight() / 2;
        if (x < 0 || z < 0 || x >= this.image.getWidth() || z >= this.image.getHeight()) return this.defaultBiome;
        this.image.loadPixelsInRange(x, z, false, Atlas.GEN_RADIUS);
        return this.biomes.getOrDefault(this.image.getPixels()[z][x], this.defaultBiome);
    }

    public record BiomeEntry(RegistryEntry<Biome> biome, int color) {
        public static final Codec<BiomeEntry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                Biome.REGISTRY_CODEC.fieldOf("biome").forGetter(BiomeEntry::biome),
                Codec.INT.fieldOf("color").forGetter(BiomeEntry::color)
        ).apply(instance, BiomeEntry::new));
    }
}

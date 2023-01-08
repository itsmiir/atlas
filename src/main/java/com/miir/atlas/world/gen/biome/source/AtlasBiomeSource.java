package com.miir.atlas.world.gen.biome.source;

import com.miir.atlas.Atlas;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import javax.imageio.ImageIO;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AtlasBiomeSource extends BiomeSource {
    private int width;
    private int height;
    private final String path;
    private int[][] biomePixels;
    private final List<BiomeEntry> biomeEntries;
    private final RegistryEntry<Biome> defaultBiome;
    private final Int2ObjectArrayMap<RegistryEntry<Biome>> biomes = new Int2ObjectArrayMap<>();

    protected AtlasBiomeSource(String path, List<BiomeEntry> biomes, RegistryEntry<Biome> defaultBiome) {
        super(biomes.stream().map(biomeEntry -> biomeEntry.biome).toList());
        this.path = path;
        this.biomeEntries = biomes;
        this.defaultBiome = defaultBiome;
        for (BiomeEntry entry : this.biomeEntries) this.biomes.put(entry.color, entry.biome);
    }

    public static final Codec<AtlasBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("path").forGetter(AtlasBiomeSource::getPath),
            Codecs.nonEmptyList(BiomeEntry.CODEC.listOf()).fieldOf("biomes").forGetter(AtlasBiomeSource::getBiomeEntries),
            Biome.REGISTRY_CODEC.fieldOf("default").forGetter(AtlasBiomeSource::getDefaultBiome)
    ).apply(instance, AtlasBiomeSource::new));

    public List<BiomeEntry> getBiomeEntries() {
        return this.biomeEntries;
    }
    public RegistryEntry<Biome> getDefaultBiome(){return this.defaultBiome;}
    public String getPath() {
        return this.path;
    }

    @Override
    protected Codec<AtlasBiomeSource> getCodec() {
        return CODEC;
    }

    public void findBiomeMap(String levelName) {
        String path = FabricLoader.getInstance().getConfigDir().toString() + "\\" + Atlas.MOD_ID + "\\" + levelName + "\\" + this.path + "\\" + "biomes.png";
        try {
            Raster raster = ImageIO.read(new File(path)).getData();
            // top left == (0,0)
            this.width = raster.getWidth();
            if (this.width % 2 != 0) width -= 1;
            this.height = raster.getHeight();
            if (this.height % 2 != 0) height -= 1;
            int[] data = raster.getPixels(0, 0, width, height, (int[]) null);
            this.biomePixels = new int[height][width];
            int x = 0;
            int y = 0;
            for (int i = 0; i < data.length; i++) {
                if (x >= width) {
                    x = 0;
                    if(++y >= height) break;
                }
                if (i % 4 == 0) {
                    int l = data[i] << 16 | data[i+1] << 8 | data[i+2];
                    this.biomePixels[y][x++] = l;
                }
            }
            Atlas.LOGGER.info("found biomes for dimension " + this.path + " in a " + width + "x" + height + " map!");
        } catch (IOException ioe) {
            throw new IllegalStateException("could not find biome map file at" + path + "!");
        }
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        x *=4;
        z *=4;
        x += width / 2;
        z += height / 2;
        if (x < 0 || z < 0 || x >= width || z >= height) return this.defaultBiome;
        return this.biomes.getOrDefault(this.biomePixels[z][x], this.defaultBiome);
    }

    public record BiomeEntry(RegistryEntry<Biome> biome, int color) {
        public static final Codec<BiomeEntry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                Biome.REGISTRY_CODEC.fieldOf("biome").forGetter(BiomeEntry::biome),
                Codec.INT.fieldOf("color").forGetter(BiomeEntry::color)
        ).apply(instance, BiomeEntry::new));
    }
}

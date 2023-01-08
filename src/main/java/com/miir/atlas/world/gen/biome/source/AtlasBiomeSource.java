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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AtlasBiomeSource extends BiomeSource {
    private int width;
    private int height;
    private final String path;
    private int[][] biomePixels;
    private final List<BiomeEntry> biomeEntries;
    private final Int2ObjectArrayMap<RegistryEntry<Biome>> biomes = new Int2ObjectArrayMap<>();

    protected AtlasBiomeSource(String path, List<BiomeEntry> biomes) {
        super(biomes.stream().flatMap(biomeEntry -> Stream.of(biomeEntry.biome)).collect(Collectors.toList()));
        this.path = path;
        this.biomeEntries = biomes;
        for (BiomeEntry entry : this.biomeEntries) this.biomes.put(entry.color, entry.biome);
        this.findBiomeMap("test");
    }

    public static final Codec<AtlasBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("path").forGetter(AtlasBiomeSource::getPath),
            Codecs.nonEmptyList(BiomeEntry.CODEC.listOf()).fieldOf("biomes").forGetter(AtlasBiomeSource::getBiomeEntries)
    ).apply(instance, AtlasBiomeSource::new));

    public List<BiomeEntry> getBiomeEntries() {
        return this.biomeEntries;
    }

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
                    int l = data[i] << 24 | data[i+1] << 16 | data[i+2] << 8 | data[i+3];
                    this.biomePixels[y][x++] = l;
                }
            }
            Atlas.LOGGER.info("imported a map for dimension " + this.path + " with a " + width + "x" + height + " map!");
        } catch (IOException ioe) {
            throw new IllegalStateException("could not find biome map file at" + path + "!");
        }
        if (!this.biomes.containsKey(255)) {
            throw new IllegalStateException("biome listing for dimension "+this.path+" should contain a default entry mapped to 255!");
        }
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        x *=4;
        z *=4;
        x += width / 2;
        z += height / 2;
        if (x < 0 || z < 0 || x >= width || z >= height) return biomes.get(255);
//        Atlas.LOGGER.info(this.biomes.getOrDefault(this.biomePixels[z][x], this.biomes.get(255)).toString());
        return this.biomes.getOrDefault(this.biomePixels[z][x], this.biomes.get(255));
    }

    public record BiomeEntry(RegistryEntry<Biome> biome, int color) {
        public static final Codec<BiomeEntry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                Biome.REGISTRY_CODEC.fieldOf("biome").forGetter(BiomeEntry::biome),
                Codec.INT.fieldOf("color").forGetter(BiomeEntry::color)
        ).apply(instance, BiomeEntry::new));
    }
}

package com.miir.atlas.world.gen.noise;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.NamespacedMapImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public class ImageDensityFunction implements DensityFunction {
    public static final Codec<ImageDensityFunction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("path").forGetter(ImageDensityFunction::path)
    ) .apply(instance, ImageDensityFunction::new));
    private static final CodecHolder<ImageDensityFunction> CODEC_HOLDER = CodecHolder.of(CODEC);
    private NamespacedMapImage map;
    private final String path;

    public ImageDensityFunction(String path) {
        this.path = path;
        this.map = null;
    }
    private ImageDensityFunction(NamespacedMapImage map, String path) {
        if (map == null) map = new NamespacedMapImage(Atlas.MAPS.get(path));
        this.map = map;
        this.path = map.getPath();
    }

    public String path() {return this.path;}

    @Override
    public double sample(NoisePos pos) {
        if (this.map == null) {
            this.map = new NamespacedMapImage(Atlas.MAPS.get(path));
        }
        if (this.map == null) throw new IllegalStateException();
            return this.map.transformToNoise(pos.blockX(), pos.blockY(), pos.blockZ());
    }

    @Override
    public void applyEach(double[] densities, EachApplier applier) {
        applier.applyEach(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new ImageDensityFunction(this.map, this.path));
    }

    @Override
    public double minValue() {
        return -2;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}

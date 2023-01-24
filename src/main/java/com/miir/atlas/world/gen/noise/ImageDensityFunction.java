package com.miir.atlas.world.gen.noise;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.NamespacedMapImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;

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
    private ImageDensityFunction(NamespacedMapImage map, String path) throws IOException {
        if (map == null) map = new NamespacedMapImage(
                Atlas.MAPS.getOrDefault(
                        path,
                        new NamespacedMapImage(path, NamespacedMapImage.Type.GRAYSCALE).initialize(Atlas.SERVER)));
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
            return this.map.transformToNoise(pos.blockX(), pos.blockY()*4, pos.blockZ());
    }

    @Override
    public void applyEach(double[] densities, EachApplier applier) {
        applier.applyEach(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        try {
            return visitor.apply(new ImageDensityFunction(this.map, this.path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("error loading map for image density function at "+this.path);
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

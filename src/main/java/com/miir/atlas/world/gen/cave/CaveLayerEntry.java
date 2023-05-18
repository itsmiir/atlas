package com.miir.atlas.world.gen.cave;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.NamespacedMapImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

@Deprecated
public class CaveLayerEntry {
    private final NamespacedMapImage ceiling;
    private final NamespacedMapImage floor;
    private NamespacedMapImage biomes;
    private NamespacedMapImage aquifer;
    private final int verticalScale;
    private final int floorHeight ;
    private final int ceilingHeight;
    private final String biomePath;
    private final String floorPath;
    private final String ceilingPath;
    private final String name;

    public CaveLayerEntry(String name, String ceilingPath, String floorPath, String biomePath, int ceilingHeight, int floorHeight, int verticalScale) {
        this.name = name;
        this.verticalScale = verticalScale;
        this.floorHeight = floorHeight;
        this.ceilingHeight = ceilingHeight;
        this.biomePath = biomePath;
        this.ceilingPath = ceilingPath;
        this.floorPath = floorPath;
        this.ceiling = Atlas.getOrCreateMap(ceilingPath, NamespacedMapImage.Type.GRAYSCALE);
        this.floor = Atlas.getOrCreateMap(floorPath, NamespacedMapImage.Type.GRAYSCALE);
        if (!this.biomePath.equals("")) {
            this.biomes = Atlas.getOrCreateMap(biomePath, NamespacedMapImage.Type.COLOR);
        }
    }
    public static final Codec<CaveLayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(CaveLayerEntry::getName),
            Codec.STRING.fieldOf("ceiling").forGetter(CaveLayerEntry::ceilingPath),
            Codec.STRING.fieldOf("floor").forGetter(CaveLayerEntry::floorPath),
            Codec.STRING.optionalFieldOf("biomes", "").forGetter(CaveLayerEntry::biomePath),
            Codec.INT.fieldOf("ceiling_height").forGetter(CaveLayerEntry::ceilingHeight),
            Codec.INT.fieldOf("floor_height").forGetter(CaveLayerEntry::floorHeight),
            Codec.INT.optionalFieldOf("vertical_scale", 0).forGetter(CaveLayerEntry::verticalScale)
            ).apply(instance, CaveLayerEntry::new));

    private String getName() {return this.name;}
    private int verticalScale() {return this.verticalScale;}
    private String biomePath() {return this.biomePath;}
    private String floorPath() {return this.floorPath;}
    private String ceilingPath() {return this.ceilingPath;}

    public int floorHeight() {return this.floorHeight;}
    public int ceilingHeight() {return this.ceilingHeight;}

    public NamespacedMapImage getCeiling() {return this.ceiling;}
    public NamespacedMapImage getFloor() {return this.floor;}
    @Nullable public NamespacedMapImage getBiomes() {return this.biomes;}
    @Nullable public NamespacedMapImage getAquifer() {return this.aquifer;}
}

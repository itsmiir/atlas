package com.miir.atlas.world.gen.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;

import java.util.List;
import java.util.Optional;

public class BiomeEntry {
    /*public static final Biome EMPTY = new Biome.Builder()
            .precipitation(Biome.Precipitation.NONE)
            .temperature(0.5f)
            .downfall(0.5f)
            .effects(new BiomeEffects.Builder()
                    .build())
            .build();*/

    public static final Identifier EMPTY = new Identifier("the_void");
    public static final BiomeEntryPriorityCodec PRIORITY_CODEC = new BiomeEntryPriorityCodec();

    private final Optional<RegistryEntry<Biome>> biome;
    private final Optional<List<RegistryEntry<Biome>>> priority;
    private final int color;

    private final RegistryEntry<Biome> topBiome;

    public BiomeEntry(Optional<RegistryEntry<Biome>> biome, Optional<List<RegistryEntry<Biome>>> priority, int color) {
        this.biome = biome;
        this.priority = priority;
        this.color = color;
        topBiome = getTopBiome();
    }

    public static final Codec<BiomeEntry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Biome.REGISTRY_CODEC.optionalFieldOf("biome").forGetter(BiomeEntry::getBiome),
            PRIORITY_CODEC.listOf().optionalFieldOf("priority").forGetter(BiomeEntry::getPriority),
            Codec.INT.fieldOf("color").forGetter(BiomeEntry::getColor)
    ).apply(instance, BiomeEntry::new));

    public RegistryEntry<Biome> getTopBiome() {
        if (biome.isPresent()) {
            return biome.get();
        }
        if (priority.isEmpty()) {
            throw new IllegalStateException("biome entry for color " + color + " must specify either 'biome' key or 'priority' list");
        }
        for (RegistryEntry<Biome> priorityEntry : priority.get()) {
            System.out.println(priorityEntry);
            if (!priorityEntry.matchesId(EMPTY)) {
                return priorityEntry;
            }
        }
        throw new IllegalStateException("invalid final-priority biome for color " + color);
    }

    public Optional<RegistryEntry<Biome>> getBiome() {
        return biome;
    }

    public Optional<List<RegistryEntry<Biome>>> getPriority() {
        return priority.map(entries -> List.of(topBiome));
    }

    public int getColor() {
        return color;
    }
}

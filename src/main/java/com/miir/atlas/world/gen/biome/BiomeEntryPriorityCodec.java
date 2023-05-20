package com.miir.atlas.world.gen.biome;

import com.miir.atlas.mixin.RegistryElementCodecAccessor;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.Optional;

public class BiomeEntryPriorityCodec extends RegistryElementCodec<Biome> {

    public BiomeEntryPriorityCodec() {
        super(RegistryKeys.BIOME, Biome.CODEC, false);
    }

    public RegistryEntry.Reference<Biome> getEmpty(RegistryElementCodecAccessor<Biome> accessor, RegistryEntryLookup<Biome> registryEntryLookup) {
        RegistryKey<Biome> registryKey = RegistryKey.of(accessor.getRegistryRef(), BiomeEntry.EMPTY);
        return registryEntryLookup.getOptional(registryKey).get();
    }

    @Override
    public <T> DataResult<Pair<RegistryEntry<Biome>, T>> decode(DynamicOps<T> ops, T input) {
        RegistryElementCodecAccessor<Biome> accessor = (RegistryElementCodecAccessor<Biome>) this;
        if (ops instanceof RegistryOps registryOps) {
            Optional<RegistryEntryLookup<Biome>> optional = registryOps.getEntryLookup(accessor.getRegistryRef());
            if (optional.isEmpty()) {
                return DataResult.error(() -> ("Registry does not exist: " + accessor.getRegistryRef()));
            }
            RegistryEntryLookup<Biome> registryEntryLookup = optional.get();
            DataResult<Pair<Identifier, T>> dataResult = Identifier.CODEC.decode(ops, input);
            Pair<Identifier, T> pair2 = dataResult.getOrThrow(false, s -> { throw new IllegalStateException(s); });
            RegistryKey<Biome> registryKey = RegistryKey.of(accessor.getRegistryRef(), pair2.getFirst());
            Optional<RegistryEntry.Reference<Biome>> result = registryEntryLookup.getOptional(registryKey);
            return DataResult.success(result.orElse(getEmpty(accessor, registryEntryLookup))).map(reference -> Pair.of(reference, pair2.getSecond()));
        }
        return accessor.getElementCodec().decode(ops, input).map(pair -> pair.mapFirst(RegistryEntry::of));
    }
}

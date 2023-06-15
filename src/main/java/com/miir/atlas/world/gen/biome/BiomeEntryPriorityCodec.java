package com.miir.atlas.world.gen.biome;

import com.miir.atlas.mixin.RegistryElementListCodecAccessor;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.RegistryElementCodec;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.*;
import net.minecraft.world.biome.Biome;

import java.util.Optional;

public class BiomeEntryPriorityCodec extends RegistryElementCodec<Biome> {

    public BiomeEntryPriorityCodec() {
        super(Registry.BIOME_KEY, Biome.CODEC, false);
    }

    public RegistryEntry<Biome> getEmpty(RegistryElementListCodecAccessor<Biome> accessor, Registry<Biome> registry) {
        RegistryKey<Biome> registryKey = RegistryKey.of(accessor.getRegistry(), BiomeEntry.EMPTY);
        return registry.getEntry(registryKey).get();
    }

    @Override
    public <T> DataResult<Pair<RegistryEntry<Biome>, T>> decode(DynamicOps<T> ops, T input) {
        RegistryElementListCodecAccessor<Biome> accessor = (RegistryElementListCodecAccessor<Biome>) this;
        if (ops instanceof RegistryOps registryOps) {
            Optional<Registry<Biome>> optional = registryOps.getRegistry(accessor.getRegistry());
            if (optional.isEmpty()) {
                return DataResult.error("Registry does not exist: " + accessor.getRegistry());
            }
            Registry<Biome> registryEntryLookup = optional.get();
            DataResult<Pair<Identifier, T>> dataResult = Identifier.CODEC.decode(ops, input);
            Pair<Identifier, T> pair2 = dataResult.getOrThrow(false, s -> { throw new IllegalStateException(s); });
            RegistryKey<Biome> registryKey = RegistryKey.of(accessor.getRegistry(), pair2.getFirst());
            Optional<RegistryEntry<Biome>> result = registryEntryLookup.getEntry(registryKey);
            return DataResult.success(result.orElse(getEmpty(accessor, registryEntryLookup))).map(reference -> Pair.of(reference, pair2.getSecond()));
        }
        return accessor.getEntryCodec().decode(ops, input);
    }
}

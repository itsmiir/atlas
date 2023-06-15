package com.miir.atlas.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryListCodec;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(RegistryEntryListCodec.class)
public interface RegistryElementListCodecAccessor<E> {

    @Accessor
    RegistryKey<? extends Registry<E>> getRegistry();

    @Accessor
    Codec<RegistryEntry<E>> getEntryCodec();
}

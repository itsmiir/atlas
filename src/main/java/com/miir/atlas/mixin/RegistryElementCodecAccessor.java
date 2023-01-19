package com.miir.atlas.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryElementCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RegistryElementCodec.class)
public interface RegistryElementCodecAccessor<E> {

    @Accessor
    RegistryKey<? extends Registry<E>> getRegistryRef();

    @Accessor
    Codec<E> getElementCodec();
}

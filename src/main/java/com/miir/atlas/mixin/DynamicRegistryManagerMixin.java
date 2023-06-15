package com.miir.atlas.mixin;

import com.google.common.collect.ImmutableMap;
import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.AtlasMapInfo;
import net.minecraft.util.registry.DynamicRegistryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/*
 * This is used instead of access wideners because interface fields are always final and this cannot be changed.
 */
@Mixin(DynamicRegistryManager.class)
public interface DynamicRegistryManagerMixin {
    @Inject(method = "method_30531", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap$Builder;build()Lcom/google/common/collect/ImmutableMap;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void atlas_addDynamicRegistry(CallbackInfoReturnable<ImmutableMap> cir, ImmutableMap.Builder builder) {
        DynamicRegistryManager.register(builder, Atlas.ATLAS_INFO, AtlasMapInfo.CODEC);
    }
}

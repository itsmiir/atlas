package com.miir.atlas.mixin;

import com.miir.atlas.world.gen.biome.source.AtlasBiomeSource;
import com.miir.atlas.world.gen.chunk.AtlasChunkGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SaveProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract Iterable<ServerWorld> getWorlds();
    @Shadow @Final protected SaveProperties saveProperties;

    @Inject(method = "createWorlds", at = @At("TAIL"))
    private void atlas_grabServer(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        String levelName = this.saveProperties.getLevelName();
        for (ServerWorld world : this.getWorlds()) {
            if (world.getChunkManager().getChunkGenerator() instanceof AtlasChunkGenerator cg) {
                cg.findHeightmap(levelName);
                if (cg.getBiomeSource() instanceof AtlasBiomeSource abs) {
                    abs.findBiomeMap(levelName);
                }
            }
        }
    }
}

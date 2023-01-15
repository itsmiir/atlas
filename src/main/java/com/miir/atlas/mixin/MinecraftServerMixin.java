package com.miir.atlas.mixin;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.biome.source.AtlasBiomeSource;
import com.miir.atlas.world.gen.chunk.AtlasChunkGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract Iterable<ServerWorld> getWorlds();

    @Inject(method = "createWorlds", at = @At("TAIL"))
    private void atlas_grabServer(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        Atlas.SERVER = server;
        for (ServerWorld world : this.getWorlds()) {
            if (world.getChunkManager().getChunkGenerator() instanceof AtlasChunkGenerator cg) {
                String dimensionName = world.getRegistryKey().getValue().toString();
                try {
                    cg.findMaps(server, dimensionName);
                    if (cg.getBiomeSource() instanceof AtlasBiomeSource abs) {
                        abs.findBiomeMap(server, dimensionName);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}

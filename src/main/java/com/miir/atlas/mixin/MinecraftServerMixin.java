package com.miir.atlas.mixin;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.chunk.AtlasChunkGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.SaveProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract Iterable<ServerWorld> getWorlds();

    @Shadow public abstract Path getSavePath(WorldSavePath worldSavePath);

    @Shadow @Final protected SaveProperties saveProperties;

    @Inject(method = "createWorlds", at = @At("TAIL"))
    private void atlas_grabServer(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        Atlas.LEVEL_NAME = this.saveProperties.getLevelInfo().getLevelName();
        for (ServerWorld world : this.getWorlds()) {
            if (world.getChunkManager().getChunkGenerator() instanceof AtlasChunkGenerator cg) {
                Identifier path = world.getDimensionKey().getValue();
                cg.findHeightmap(Atlas.LEVEL_NAME);
//                cg.findBiomeMap();
            }
        }
    }
}

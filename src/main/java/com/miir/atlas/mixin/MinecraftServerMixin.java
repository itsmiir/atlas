package com.miir.atlas.mixin;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.biome.source.AtlasBiomeSource;
import com.miir.atlas.world.gen.chunk.AtlasChunkGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.dimension.DimensionOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow @Final protected SaveProperties saveProperties;

    @Inject(method = "createWorlds", at = @At("HEAD"))
    private void atlas_grabServer(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        Atlas.SERVER = server;
        Registry<DimensionOptions> registry = this.saveProperties.getGeneratorOptions().getDimensions();
        for (Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry : registry.getEntrySet()) {
            if (entry.getValue().getChunkGenerator().getBiomeSource() instanceof AtlasBiomeSource abs) {
                try {
                    abs.findBiomeMap(server, entry.getKey().getValue().toString());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            if (entry.getValue().getChunkGenerator() instanceof AtlasChunkGenerator cg) {
                try {
                    cg.findMaps(server, entry.getKey().getValue().toString());
                } catch (Exception e) {
                    throw new IllegalStateException("error initializing: could not find maps for dimension "+entry.getKey().getValue().toString());
                }
            }
        }
    }
}

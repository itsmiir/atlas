package com.miir.atlas.mixin;

import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GenerationShapeConfig.class)
public class GenerationShapeConfigMixin {
    @Shadow @Final private int horizontalSize;

    @Shadow @Final private int verticalSize;

    @Overwrite
    public int horizontalBlockSize() {
//        return this.horizontalSize;
        return 1;
    }
    @Overwrite
    public int verticalBlockSize() {
//        return this.verticalSize;
        return 1;
    }
}

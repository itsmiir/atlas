package com.miir.atlas.mixin;

import com.miir.atlas.accessor.MapInfoAccessor;
import com.miir.atlas.world.gen.AtlasMapInfo;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MaterialRules.MaterialRuleContext.class)
public class MaterialRuleContextMixin implements MapInfoAccessor {
    @Unique
    private RegistryEntry<AtlasMapInfo> atlas_AMI;

    @Override
    public RegistryEntry<AtlasMapInfo> atlas_getAMI() {
        return this.atlas_AMI;
    }

    @Override
    public void atlas_setAMI(RegistryEntry<AtlasMapInfo> ami) {
        this.atlas_AMI = ami;
    }


}

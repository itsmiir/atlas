package com.miir.atlas.accessor;

import com.miir.atlas.world.gen.AtlasMapInfo;
import net.minecraft.util.registry.RegistryEntry;

public interface MapInfoAccessor {
    RegistryEntry<AtlasMapInfo> atlas_getAMI();
    void atlas_setAMI(RegistryEntry<AtlasMapInfo> ami);
}

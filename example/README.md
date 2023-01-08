# how to create a datapack for use with atlas

- for the `generator` option, put `atlas:atlas`.
- you can use any biome source you want, but to generate the biomes based on an iamge, use `atlas:atlas` as the biome source.

### example:
```json5
{
  "type": "minecraft:overworld",
  "generator": {
    "type": "atlas:atlas",
    "path": "avila", // this is the name of the dimension. the heightmap for this dimension
                     // should be stored at config/atlas/<world name>/avila/heightmap.json.
                     
    "settings": { ... } // see the below section for what to put here
    
    "starting_y": 6, // this is y-level that corresponds to completely black on the heightmap. 
                     // if you do not want oceans in your worlds, then set "starting_y" to 63.
                     // the maximum value of the heightmap (pure white) is "starting_y" + 255.
    "biome_source": {
      "type": "atlas:atlas",
      "path": "avila", // this is the name of the dimension. the biome image for this dimension
                       // is saved at the location config/atlas/<world name>/avila/biomes.json.
                       
      "default": "minecraft:the_void", // if the biome painter notices a color that doesn't have an associated biome,
                                       // it will default to this biome. it is also the biome that occurs outside the
                                       // generation area (i.e., the rest of the world beyond the border of the map).
                                       
      "biomes": [ // "biomes" is a list of biomes. each entry contains a reference to a biome (or, you can inline it if
                  // you like) and a color value. the color value is the decimal value of the hexcode for the color you
                  // used on the map. for example, my jagged peaks biome uses the color #FFFFFF on the map. the decimal
                  // value of FFFFFF is 16777215. you can use any biome you want here including modded/datapack biomes.
        {
          "biome": "minecraft:plains",
            "color": 7180861
        },
        {
          "biome": "minecraft:beach",
          "color": 12890200
        },
        {
          "biome": "minecraft:jungle",
          "color": 3378218
        },
        {
          "biome": "minecraft:sparse_jungle",
          "color": 7570739
        },
        {
          "biome": "minecraft:desert",
          "color": 11445578
        },
        {
          "biome": "minecraft:jagged_peaks",
          "color": 16777215
        },
        {
          "biome": "minecraft:meadow",
          "color": 8373336
        },
        {
          "biome": "minecraft:grove",
          "color": 9088392
        },
        {
          "biome": "minecraft:old_growth_spruce_taiga",
          "color": 2247968
        },
        {
          "biome": "minecraft:stony_shore",
          "color": 5395026
        },
        {
          "biome": "minecraft:ocean",
          "color": 3226251
        },
        {
          "biome": "minecraft:warm_ocean",
          "color": 4672912
        },
        {
          "biome": "minecraft:cold_ocean",
          "color": 1647754
        },
        {
          "biome": "minecraft:deep_ocean",
          "color": 1054558
        },
        {
          "biome": "minecraft:stony_peaks",
          "color": 9671571
        },
        {
          "biome": "minecraft:taiga",
          "color": 3376467
        }
      ]
    }
  }
}

```
### what to put in `"settings"`
the settings can be populated with normal settings for your dimension.
however, any density functions you put in the noise router will be completely ignored. it's recommended that you use the following:
```json5
    "settings": {
      "sea_level": 63,
      "disable_mob_generation": false,
      "aquifers_enabled": true,
      "ore_veins_enabled": true,
      "legacy_random_source": false,
      "default_block": {
        "Name": "minecraft:stone"
      },
      "default_fluid": {
        "Name": "minecraft:water",
        "Properties": {
          "level": "0"
        }
      },
      "noise": {
        "min_y": -64,
        "height": 384,
        "size_horizontal": 1,
        "size_vertical": 2
      },
      "noise_router": {
        "barrier": 0,
        "fluid_level_floodedness": 0,
        "fluid_level_spread": 0,
        "lava": 0,
        "temperature": 0,
        "vegetation": 0,
        "continents": 0,
        "erosion": 0,
        "depth": 0,
        "ridges": 0,
        "initial_density_without_jaggedness": 0,
        "final_density": {
          "type": "minecraft:interpolated",
          "argument": "minecraft:overworld/base_3d_noise"
        },
        "vein_toggle": 0,
        "vein_ridged": 0,
        "vein_gap": 0
      },
      "spawn_target": [],
      "surface_rule": { ... } // see below
  }
```
for the surface rule, because the density functions are ignored, the only change you need to make from vanilla is to remove all calls to `above_preliminary_surface`-- just unwrap whatever is inside.

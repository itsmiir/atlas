# how to create a dimension with atlas

assuming you've [set up your datapack](https://minecraft.fandom.com/wiki/Data_pack), create a new dimension file at `namespace/dimension/<name>.json`.

the example datapack included here [has a dimension file example](./avila/data/avila/dimension/avila.json) that you can simply paste and tweak if you want.

## chunk generation & heightmaps

to enable atlas for this dimension, give the chunk generator a `type` of `atlas:atlas`. the atlas generator requires a **heightmap**. this needs to be an 8bpc (bits-per-channel) RGBA png file; you can save to this format easily with the [GIMP image editor](https://www.gimp.org/). place this image in your datapack anywhere, though know that the convention is to place it in the `atlas/map` folder as `heightmap.png`, so `namespace/atlas/map/heightmap.png` for example. you then need to link to this image in the generator's `height_map` key. following the conventions, this would be `namespace:atlas/map/heightmap`.

note that you'll most likely need to adjust the `starting_y` based on the heightmap. a heightmap pixel's value corresponds to its y position without being offset by `starting_y`. this means that if the heightmap's ocean pixel color is `#272727`, with each channel being decimal `39`, and the `starting_y` is `20`, then the ocean floor will begin at y `59`. the sea level begins at y `63`, which would give you four blocks of ocean in this case.

the `vertical_scale` and `horizontal_scale` factors scale how many blocks correspond to a pixel. a `vertical_scale` of 1 means that each of the possible 256 values in the heightmap corresponds to an elevation change of one block. a `horizontal_scale` of 2 means that each pixel on the map represents a 2x2 block area ingame. **`horizontal_scale` needs to be set for both the chunk generator and the biome source.**

## biome sources & biome maps

you can use any biome source you want for the generator, but if you want to specify biomes with an image and fully take advantage of atlas, once again use `atlas:atlas`. repeat the same image procedure used for the heightmap instead using the key `biome_map` instead of `heightmap`. the convention is to name this biome image `biomes.png`, so following that, the resource location would be `namespace:atlas/map/biomes`.

atlas will read the biome map pixel-by-pixel and determine the biome of the pixel's corresponding 2d coordinate in-game by its color. atlas needs to know how to fetch biomes from colors, so we need to specify a `biomes` list. this is a list of objects, each with a `biome` key and a `color` key. the `biome` key is any biome data value. you can use any available biome, from [the vanilla biomes](https://minecraft.fandom.com/wiki/Biome) to loaded modded ones. the `color` key is the color used in the image for that specific biome in its integer representation; for example, `#FFFFFF` is `16777215`. you can convert hex to decimal [with this tool](https://www.rapidtables.com/convert/number/hex-to-decimal.html).

you can also specify a default biome under the `default` key for when atlas encounters a pixel with no matching color in the `biomes` key. 

## advanced: what to put in generator `settings`

the settings can be populated with normal settings for your dimension; however, any density functions you put in the noise router will be completely ignored. it's recommended that you use the following:

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

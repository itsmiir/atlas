# how to create a dimension with atlas

## quick start/TL;DR:
- copy the example datapack, avila
- replace the biomes.png and heightmap.png files with your own biome and heightmap
- ???
- profit

## full tutorial

assuming you've [set up your datapack](https://minecraft.fandom.com/wiki/Data_pack), create a new dimension file at
`namespace/dimension/<name>.json`. like all datapacks, you can put your file in the `minecraft` namespace and overwrite
the vanilla dimensions if you want.

the example datapack included here [has a dimension file example](./avila/data/avila/dimension/avila.json) that you can
simply paste and tweak if you want. let's look at that file now:

`data/avila/dimension/avila.json`
```json5
{
  "type": "minecraft:overworld",
  "generator": {
    "type": "atlas:atlas",
    "map_info": "avila:avila",
    "biome_source": {
      "type": "atlas:atlas",
      "map_info": "avila:avila",
      "biome_map": "avila:atlas/map/biomes",
      "default": "minecraft:the_void",
      "biomes": [
        {
          "biome": "minecraft:plains",
          "color": 7180861
        },
        {
          "biome": "minecraft:beach",
          "color": 12890200
        },
        ...
      ],
      "cave_biomes": [
        ...
      ],
      "below_depth": 10
    },
    "settings": "atlas:default"
  }
}
```

let's break down what each part of this file does.
## dimension type

the `type` field defines certain parameters about your world unrelated to atlas. it's a vanilla feature. when in doubt,
just set it to `minecraft:overworld`. if you want to go deeper, research `dimension types`-- again, this is a vanilla
feature. we're much more interested in the `generator` field.

## generator

to tell the game that you want this to be an atlas dimension, we set the `type` field *inside the `generator` field* to `atlas:atlas`.

### map info

`map_info` is a field that contains information about the terrain generation settings the mod should use. you can define
it inline, but it's much easier to define it in its own seperate file, since you'll need to use it more than once per
dimension. if you choose to go this route, a `map_info` should be stored at `/data/<namespace>/worldgen/atlas_map_info/path`.
so in this example, the `map_info` `avila:avila` is stored [here](./avila/data/avila/worldgen/atlas_map_info/avila.json).
see the "map info explained" section for how to configure this.

### settings

this field controls more nitty-gritty aspects of your world. default block, default fluid, et cetera. to make this process
easier, you can just use one of the three provided presets: `atlas:default`, `atlas:no_entrances`, and `atlas:no_noise_caves`.
they're pretty self-explanatory-- `default` is a normal world; `no_entrances` will remove the noise cave entrances from 
your world, giving you a cleaner surface, a nd `no_noise_caves` disables all noise-based cave, except the pre-1.18 caves
that cut through the world regardless. if you want absolutely no caves, you'll need to remove the carvers from each
biome you use. this isn't too hard; it's just a ton of copy-pasting json files. see the "manually modifying `settings`"
section for some notes on how to modify this section more.

### biome source

the `biome_source` field controls how the biomes spawn in the world. the `type` should be `atlas:atlas`. the `map_info`
field should be the same as the previous one. 

the `default` field is the name of the default biome the game should fall back on when no other biome fits. usually,
`minecraft:the_void` is sufficient for this.

`biome_map` is the path to your biome map PNG file. it can be anywhere in the datapack.

`biomes` is a list of biomes that you want to have in your map. each biome entry has a `color` field and a `biome` field;
these correspond to the color on the biome map that you want your biome to spawn at, and the name of the biome you want
to spawn there.

#### optional biome source fields:
`cave_biomes` is a set of biomes that will populate the underground. these biomes are generated using the default world
generator, and as such should be listed in vanilla's biome format. in theory, these biomes could be anything you want,
but most people will want to simply copy and paste this field from the [example](./avila/data/avila/dimension/avila.json).
the configuration in the default file will generate cave biomes as they are in the vanilla game.

`below_depth` is the depth at which the cave biomes start spawning. for example, a `below_depth` of 10 means that cave
biomes can generate a minimum of ten blocks below the surface. in practice, this is uncommon-- it's best (and most accurate
to vanilla) to keep this value as low as possible.

### map info explained

the `map_info` json file looks like this:

`/data/avila/worldgen/atlas_map_info/avila.json`
```json5
{
  "height_map": "avila:atlas/map/heightmap",
  "starting_y": 6,
  "horizontal_scale": 1,
  "vertical_scale": 1
}
```
this object tells the game important information about the map:

the `height_map` parameter tells the game where to find the heightmap file of the world in your datapack. it can be
anywhere in the datapack.

the `starting_y` parameter tells the game how high the lowest block in the heightmap should be-- i.e., `starting_y` is
the elevation that a black pixel on the heightmap represents in-game.

the `horizontal_` and `vertical_scale` parameters how to scale the image in-game. for example, a world with a both
parameters set to 1 will mean that the world is as many blocks across as there are pixels, and that the lowest point on
the surface will be 255 blocks below the highest point. a world with `horizontal_scale` set to 2 and `vertical_scale`
set to 3 will mean that the world will be twice as many blocks across as there are pixels in the image, and the lowest
point will be 768 blocks below the highest point. **these features are experimental! it's much preferred to do this
scaling in your image file beforehand.**

### other optional fields

#### aquifers

`aquifer` is an optional parameter for the `generator` object that allows you to specify the sea level at each point on
the map. it works the exact same as a heightmap, but corresponds to sea level instead of terrain elevation.

aquifers let you define the sea level at any point in the world. they work exactly the same as heightmaps. the sea level
at any given coordinate is calculated as the minimum of `sea_level` and the aquifer value at that point. if you want to
use an aquifer, add an `aquifer` field to your chunk generator and specify a path to the aquifer image, like so:
```json5
{
  "generator": {
    "type": "atlas:atlas",
    "aquifer": "my_datapack:path/to/PNGfile"
  }
}
```
if you don't include this field, the generator will default to using the sea level everywhere.

#### conditional biomes

in some cases, you may be working an environment where some biomes may not be loaded-- for example, if you want to use a
biome from another datapack, without including a hard dependency on that datapack. in that case, you can replace a biome
entry with a priority list, like so:
```json5
[ // list of biomes
  ...
  {
    // previous biome entry
  },
  {
    "priority": [
      "first_datapack:super_plains",
      "second_datapack:better_plains",
      "third_datapack:plains_two",
      "minecraft:plains"
    ],
    "color": 5081666
  },
  {
    // next biome entry
  },
  ...
]
```
atlas will check if `first_datapack:super_plains` is loaded. if so, it will use that biome for that color. if not, it
will check if `second_datapack:better_plains` is loaded, et cetera. **it is important to always end this list with a
vanilla biome, so that the world generator always has something to fall back on.**

#### manually modifying `settings`

modifying the `settings` field allows you to have more control over how the world generates. here you can change things
like default sea level, as well as manually modify cave generation. from a technical level, the world generates identically
to a vanilla noise-based chunk generator below `below_depth` blocks beneath the surface, with a few key differences:
- in order to have surface rules apply properly, you need to use `atlas:above_preliminary_surface` instead of
`minecraft:above_preliminary_surface` in your surface rule.
- the `initial_density_without_jaggedness` function is what gets added to the surface generation to create cave entrances;
setting this to 1 will remove them entirely, and setting it to 0 will remove all surface terrain.
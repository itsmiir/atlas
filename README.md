# Atlas
atlas is a data-driven image-based world generator. give the mod a heightmap and biome map image, and you can incorporate it into a [datapack dimension](https://minecraft.fandom.com/wiki/Custom_dimension)! 

### how to use
- create or find a grayscale heightmap. save it as a png with 8bpc, RGBA encoding. the image should be saved in your minecraft directory at `/config/atlas/<world name>/<dimension name>/heightmap.png`.
![A grayscale heightmap of the Avila mountains. The mountain range appears as a white, branching line across the map. Image courtesy Irene Alvarado, https://medium.com/energeia/printing-mountains-6bbf577294b6](https://cdn.discordapp.com/attachments/769711740366880789/1061717611857051708/heightmap.png)
- create a copy of the image and paint over it with whatever colors you like. each color corresponds to a different biome. do not mix or blend the colors. save this image with the same encoding to `/config/atlas/<world name>/<dimension name>/biomes.png`.
![An image of the same terrain, overlaid with roughly a dozen discrete colors, each corresponding to a particular biome.](https://cdn.discordapp.com/attachments/769711740366880789/1061717611487961170/biomes.png)
- create a datapack and put your dimension file in as normal. **an example dimension file, along with an explanation of the parameters, can be found [here](https://github.com/itsmiir/atlas/tree/1.19/example).**
- load up the world! if something has gone wrong, you'll get a datapack error in your logs. 
![A screenshot from inside Minecraft. The player is standing on a small hill looking up at a mountain whose biomes and elevation correspond to the previous images.](https://cdn.discordapp.com/attachments/769711740366880789/1061718435085697104/2023-01-08_12.38.20.png)

if you're still having trouble, send a message in the [discord](https://discord.gg/6p27K23zSa) `#help-and-support` channel.

### some tips:
- when you're drawing the biomes, it may be helpful to be able to see the heightmap as a [contour map](https://en.wikipedia.org/wiki/Contour_line). you can emulate this in your preferred photo editor by selecting a black pixel and then using "select by color" with varying thresholds to get varying contour lines of your map.
- if your maps are not the same size, you might have regions of void that are mapped to biomes, or regions of terrain that are just your default biome. you can use this to save yourself some time!
- your map will be centered at 0,0. up in the image is north. if your map is an uneven number of pixels, the last pixel on the south and/or east side will be cut off.
- if `starting_y` in your dimension is less than your dimension's `min_y`, you can create areas of void. use this to create non-rectangular maps!
- anything outside of the world will be void, but certain hardcoded structures may still spawn, depending on what features are in your default biome.

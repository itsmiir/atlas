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

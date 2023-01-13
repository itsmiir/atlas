package com.miir.atlas.world.gen;

import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;

public class NamespacedMapImage {

    public enum Type {
        HEIGHTMAP,
        BIOMES
    }

    private final String path;
    private final Type type;

    private int width;
    private int height;
    private int[][] pixels;

    public NamespacedMapImage(String path, Type type) {
        this.path = path;
        this.type = type;
    }

    private Raster getRasterImage(MinecraftServer server) throws IOException {
        Identifier id = new Identifier(this.path + ".png");
        Resource imageResource = server.getResourceManager()
                .getResource(id)
                .orElse(null);
        if (imageResource == null) {
            throw new IOException("could not find " + id);
        }
        BufferedImage image = ImageIO.read(imageResource.getInputStream());
        return image.getData();
    }

    public void initialize(MinecraftServer server) throws IOException {
        Raster raster = getRasterImage(server);
        this.width = raster.getWidth();
        if (this.width % 2 != 0) width -=1;
        this.height = raster.getHeight();
        if (this.height % 2 != 0) height -=1;
        this.pixels = new int[height][width];
        populate(raster);
    }

    private void populate(Raster raster) {
        switch (this.type) {
            case HEIGHTMAP -> populateHeightmapPixels(raster);
            case BIOMES -> populateBiomePixels(raster);
        }
    }

    private void populateHeightmapPixels(Raster raster) {
        int[] data = raster.getPixels(0, 0, this.width, this.height, (int[]) null);
        int x = 0;
        int y = 0;
        for (int i = 0; i < data.length; i++) {
            if (x >= width) {
                x = 0;
                y++;
            }
            if (i % 4 == 0) this.pixels[y][x++] = data[i];
        }
    }

    private void populateBiomePixels(Raster raster) {
        int[] data = raster.getPixels(0, 0, this.width, this.height, (int[]) null);
        int x = 0;
        int y = 0;
        for (int i = 0; i < data.length; i++) {
            if (x >= width) {
                x = 0;
                if(++y >= height) break;
            }
            if (i % 4 == 0) {
                int l = data[i] << 16 | data[i+1] << 8 | data[i+2];
                this.pixels[y][x++] = l;
            }
        }
    }

    public String getPath() {
        return path;
    }
    public Type getType() {return type;}
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[][] getPixels() {
        return pixels;
    }
}

package com.miir.atlas.world.gen;

import com.miir.atlas.Atlas;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;

public class NamespacedMapImage {

    private static final int EMPTY = -2;
    private boolean initialized = false;

    public enum Type {
        HEIGHTMAP,
        BIOMES,
        AQUIFER
    }

    private final String path;
    private final Type type;
    private BufferedImage image;
    private int width;
    private int height;
    private int[][] pixels;

    public NamespacedMapImage(String path, Type type) {
        this.path = path;
        this.type = type;
    }

    public int[][] loadPixelsInRange(int x, int z, boolean grayscale, int radius) {
        if (!this.initialized) {
            throw new IllegalStateException("tried to read from an uninitialized atlas!");
        }
        return this.getOrDownloadPixels(
                Math.max(0, x-radius),
                Math.max(0, z-radius),
                Math.min(this.getWidth(), x+radius),
                Math.min(this.getHeight(), z+radius), grayscale);
    }

    private int[][] getOrDownloadPixels(int x0, int z0, int x1, int z1, boolean grayscale) {
        if (this.pixels[z0][x0] == EMPTY)  {
            try {
                BufferedImage image = getImage(path, Atlas.SERVER);
                if (grayscale) {
                    populateGrayscale(image, x0, z0, x1, z1);
                } else {
                    populateColor(image, x0, z0, x1, z1);
                }
            } catch (IOException ioe) {
                Atlas.LOGGER.error("could not find map at " + path + "!");
            }
        }
        return this.pixels;
    }

    private BufferedImage getImage(String path, MinecraftServer server) throws IOException {
        if (this.image != null) {
            return image;
        }
        Identifier id = new Identifier(path + ".png");
        Resource imageResource = server.getResourceManager()
                .getResource(id)
                .orElse(null);
        if (imageResource == null) {
            throw new IOException("could not find " + id);
        }
        BufferedImage i = ImageIO.read(imageResource.getInputStream());
        this.image = i;
        return i;
    }

    public void initialize(String path, MinecraftServer server) throws IOException {
        BufferedImage image = getImage(path, server);
        this.width = image.getWidth();
        if (this.width % 2 != 0) width -=1;
        this.height = image.getHeight();
        if (this.height % 2 != 0) height -=1;
        this.pixels = new int[height][width];
        for (int[] arr : this.pixels) {
            Arrays.fill(arr, EMPTY);
        }
        this.initialized = true;
    }

    private void populate(BufferedImage image) {
        switch (this.type) {
            case HEIGHTMAP, AQUIFER -> populateGrayscale(image);
            case BIOMES -> populateColor(image);
        }
    }

    private void populateGrayscale(BufferedImage image, int x0, int z0, int x1, int z1) {
        for (int x = x0; x < x1; x++) {
            for (int y = z0; y < z1; y++) {
                this.pixels[y][x] = 0xFF & image.getRGB(x, y);
            }
        }
    }
    private void populateColor(BufferedImage image, int x0, int z0, int x1, int z1) {
        for (int x = x0; x < x1; x++) {
            for (int y = z0; y < z1; y++) {
                this.pixels[y][x] = 0xFFFFFF & image.getRGB(x, y);
            }
        }
    }
    private void populateGrayscale(BufferedImage image) {
        int[] data = new int[this.width*this.height];
        image.getRGB(0, 0, width, height, data, 0, width);
        int x = 0;
        int y = 0;
        for (int datum : data) {
            if (x >= width) {
                x = 0;
                y++;
            }
            this.pixels[y][x++] = datum & 0xFF;
        }
    }

    private void populateColor(BufferedImage image) {
        int [] data = new int[this.width*this.height];
        image.getRGB(0, 0, width, height, data, 0, width);
        int x = 0;
        int y = 0;
        for (int datum : data) {
            if (x >= width) {
                x = 0;
                y++;
            }
            this.pixels[y][x++] = datum & 0xFFFFFF;
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

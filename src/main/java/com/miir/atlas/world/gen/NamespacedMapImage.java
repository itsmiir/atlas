package com.miir.atlas.world.gen;

import com.miir.atlas.Atlas;
import com.miir.atlas.world.gen.chunk.AtlasChunkGenerator;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
        GRAYSCALE,
        COLOR
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
        if (x >= this.width || x < 0 || z > this.height || z < 0) return this.pixels;
        if (!this.initialized) {
            throw new IllegalStateException("tried to read from an uninitialized atlas!");
        }
        // todo: BufferedImage::getSubImage may be better for on-demand loading optimization
        return this.getOrDownloadPixels(
                Math.max(0, x-radius),
                Math.max(0, z-radius),
                Math.min(this.getWidth(), x+radius),
                Math.min(this.getHeight(), z+radius), grayscale);
    }

    private int[][] getOrDownloadPixels(int x0, int z0, int x1, int z1, boolean grayscale) {
        if (x0 >= this.width) x0 = this.width-1;
        if (x1 >= this.width) x1 = this.width-1;
        if (z0 >= this.height) z0 = this.height-1;
        if (z1 >= this.height) z1 = this.height-1;

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
        try {
            return this.findImage(path, server);
        } catch (IOException ioe) {
            return this.findImage(path + ".png", server);
        }
    }

    private BufferedImage findImage(String path, MinecraftServer server) throws IOException {
        if (this.image != null) {
            return image;
        }
        Identifier id = new Identifier(path);
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

    public void initialize(MinecraftServer server) throws IOException {
        try {
            getImage(this.path, server);
        } catch (IOException e) {
            getImage(this.path+".png", server);
        }
        this.width = image.getWidth();
        if (this.width % 2 != 0) width -=1;
        this.height = image.getHeight();
        if (this.height % 2 != 0) height -=1;
        this.pixels = new int[height][width];
        for (int[] arr : this.pixels) {
            Arrays.fill(arr, EMPTY);
        }
        this.initialized = true;
        this.populate(image);
    }

    private void populate(BufferedImage image) {
        switch (this.type) {
            case GRAYSCALE -> populateGrayscale(image);
            case COLOR   -> populateColor(image);
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

    public float lerp(int truncatedX, float xR, int truncatedZ, float zR) {
        int dx = 0, dz = 0;
        int u0 = Math.max(0, truncatedX + dx), v0 = Math.max(0, truncatedZ + dz);
        int u1 = Math.min(getWidth()-1, u0 + 1),    v1 = Math.min(v0 + 1, getHeight()-1);
        float i00, i01, i10, i11;
        i00 = getPixels()[v0][u0];
        i01 = getPixels()[v1][u0];
        i10 = getPixels()[v0][u1];
        i11 = getPixels()[v1][u1];
        return (float) MathHelper.lerp2(Math.abs(xR), Math.abs(zR), i00, i10, i01, i11);
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

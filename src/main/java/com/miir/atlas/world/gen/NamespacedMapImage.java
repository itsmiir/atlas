package com.miir.atlas.world.gen;

import com.miir.atlas.Atlas;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.util.Arrays;

public abstract class NamespacedMapImage<I> {
    protected I image;
    protected static final int EMPTY = -2;
    protected boolean initialized = false;

    public enum Type {
        GRAYSCALE,
        COLOR
    }

    protected final String path;
    protected final Type type;
    protected int width;
    protected int height;
    protected float[][] pixels;


    public NamespacedMapImage(String path, Type type) {
        this.path = path;
        this.type = type;
    }

    protected abstract float getR(int x, int z);
    protected abstract float getG(int x, int z);
    protected abstract float getB(int x, int z);

    public void loadPixelsInRange(int x, int z, boolean grayscale, int radius) {
        if (x >= this.width || x < 0 || z > this.height || z < 0) return;
        if (!this.initialized) {
            throw new IllegalStateException("tried to read from an uninitialized atlas!");
        }
        // todo: BufferedImage::getSubImage may be better for on-demand loading optimization
        this.getOrDownloadPixels(
                Math.max(0, x - radius),
                Math.max(0, z - radius),
                Math.min(this.getWidth(), x + radius),
                Math.min(this.getHeight(), z + radius), grayscale);
    }

    private void getOrDownloadPixels(int x0, int z0, int x1, int z1, boolean grayscale) {
        if (x0 >= this.width) x0 = this.width-1;
        if (x1 >= this.width) x1 = this.width-1;
        if (z0 >= this.height) z0 = this.height-1;
        if (z1 >= this.height) z1 = this.height-1;

        if (this.pixels[z0][x0] == EMPTY)  {
            try {
                I image = getImage(path, Atlas.SERVER);
                populate(x0, z0, x1, z1);
            } catch (IOException ioe) {
                Atlas.LOGGER.error("could not find map at " + path + "!");
            }
        }
    }

    protected abstract I getImage(String path, MinecraftServer server) throws IOException;

    public abstract void initialize(MinecraftServer server) throws IOException;

    /**
     * loads the pixel data from this map's image into its pixel array
     */
    protected void populate() {
        this.populate(0, 0, this.width, this.height);
    }
    protected abstract void populate(int startX, int startZ, int endX, int endZ);

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
    public double getElevation(int x, int z, float horizontalScale, float verticalScale, int startingY) {
        float xR = (x/horizontalScale);
        float zR = (z/horizontalScale);
        xR += this.getWidth()  / 2f; // these will always be even numbers
        zR += this.getHeight() / 2f;
        if (xR < 0 || zR < 0 || xR >= this.getWidth() || zR >= this.getHeight()) return Integer.MIN_VALUE;
        int truncatedX = (int)Math.floor(xR);
        int truncatedZ = (int)Math.floor(zR);
        double d = this.lerp(truncatedX, xR-truncatedX, truncatedZ, zR-truncatedZ);
        return verticalScale*d+ startingY;
    }

    public String getPath() {
        return path;
    }
    public Type getType() {return this.type;}
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float[][] getPixels() {
        return pixels;
    }

}

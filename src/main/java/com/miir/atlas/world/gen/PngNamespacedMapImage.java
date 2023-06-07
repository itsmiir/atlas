package com.miir.atlas.world.gen;

import com.miir.atlas.Atlas;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

public class PngNamespacedMapImage extends NamespacedMapImage<BufferedImage> {

    public PngNamespacedMapImage(String path, Type type) {
        super(path, type);
    }

    @Override
    protected float getR(int x, int z) {
        return (this.image.getRGB(x, z) >> 16);
    }

    @Override
    protected float getG(int x, int z) {
        return ((this.image.getRGB(x, z) >> 8) & 0xFF);
    }

    @Override
    protected float getB(int x, int z) {
        return ((this.image.getRGB(x, z)) & 0xFF);
    }

    public void initialize(MinecraftServer server) throws IOException {
        try {
            getImage(this.path, server);
        } catch (IOException e) {
            getImage(this.path+".png", server);
        }
        this.width = this.getWidth();
        if (this.width % 2 != 0) width -=1;
        this.height = this.getHeight();
        if (this.height % 2 != 0) height -=1;
        this.pixels = new float[height][width];
        for (float[] arr : this.pixels) {
            Arrays.fill(arr, EMPTY);
        }
        this.initialized = true;
        this.populate(image);
    }



    @Override
    protected void populate(int startX, int startZ, int endX, int endZ) {
        for (int x = startX; x < endX; x++) {
            for (int y = startZ; y < endZ; y++) {
                this.pixels[y][x++] = this.image.getRGB(x, y);
            }
        }
    }

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
    }

    protected BufferedImage getImage(String path, MinecraftServer server) throws IOException {
        try {
            return this.findImage(path, server);
        } catch (IOException ioe) {
            return this.findImage(path + ".png", server);
        }
    }

    protected BufferedImage findImage(String path, MinecraftServer server) throws IOException {
        if (this.image != null) {
            return image;
        }
        Identifier id = new Identifier(path);
        Resource imageResource = server.getResourceManager()
                .getResource(id)
                .orElse(null);
            if (imageResource == null) {
                throw new IOException("could not find " + id +"! is your image stored at that location?");
            }
        BufferedImage i = ImageIO.read(imageResource.getInputStream());
        this.image = i;
        return i;
    }

    protected void populate(BufferedImage image) {
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


}

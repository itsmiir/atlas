package com.miir.atlas.world.gen;

import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.Rasters;
import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.TiffReader;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class TiffNamespacedMapImage extends NamespacedMapImage<Rasters> {

    public TiffNamespacedMapImage(String path) {
        super(path, Type.GRAYSCALE);
    }

    @Override
    protected float getR(int x, int z) {
            return 0;
    }

    @Override
    protected float getG(int x, int z) {
        return 0;
    }

    @Override
    protected float getB(int x, int z) {
        return 0;
    }

    @Override
    protected Rasters getImage(String path, MinecraftServer server) throws IOException {
        try {
            return this.findImage(path, server);
        } catch (IOException ioe) {
            return this.findImage(path + ".tiff", server);
        }
    }

    private Rasters findImage(String path, MinecraftServer server) throws IOException {
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
        InputStream input = imageResource.getInputStream();
        TIFFImage tiffImage = TiffReader.readTiff(input);
        List<FileDirectory> directories = tiffImage.getFileDirectories();
        FileDirectory directory = directories.get(0);
        this.image = directory.readRasters();
        return this.image;
    }

    public void initialize(MinecraftServer server) throws IOException {
        try {
            getImage(this.path, server);
        } catch (IOException e) {
            getImage(this.path+".tiff", server);
        }
        this.width = image.getWidth();
        if (this.width % 2 != 0) width -=1;
        this.height = image.getHeight();
        if (this.height % 2 != 0) height -=1;
        this.pixels = new float[height][width];
        for (float[] arr : this.pixels) {
            Arrays.fill(arr, EMPTY);
        }
        this.initialized = true;
        this.populate();
    }

    @Override
    protected void populate(int startX, int startZ, int endX, int endZ) {
        for (int x = startX; x < endX; x++) {
            for (int y = startZ; y < endZ; y++) {
                this.pixels[y][x++] = this.image.getPixel(x, y)[0].floatValue();
            }
        }
    }
}

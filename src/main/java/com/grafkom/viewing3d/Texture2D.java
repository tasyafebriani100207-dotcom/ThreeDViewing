package com.grafkom.viewing3d;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/** Loads PNG/JPG classpath resources with STBImage and uploads them to OpenGL. */
public final class Texture2D implements AutoCloseable {
    private final int textureId;
    private boolean nearest;

    public Texture2D(String resourcePath) {
        byte[] imageBytes;
        try (InputStream stream = Texture2D.class.getResourceAsStream(resourcePath)) {
            if (stream == null) throw new IllegalArgumentException("Missing texture: " + resourcePath);
            imageBytes = stream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read texture: " + resourcePath, ex);
        }

        ByteBuffer encoded = MemoryUtil.memAlloc(imageBytes.length);
        encoded.put(imageBytes).flip();
        stbi_set_flip_vertically_on_load(true);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer pixels = stbi_load_from_memory(encoded, width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("STBImage could not decode " + resourcePath
                        + ": " + stbi_failure_reason());
            }
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
                    width.get(0), height.get(0), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glGenerateMipmap(GL_TEXTURE_2D);
            stbi_image_free(pixels);
        } finally {
            MemoryUtil.memFree(encoded);
        }

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        setNearest(false);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void setNearest(boolean nearest) {
        this.nearest = nearest;
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
                nearest ? GL_NEAREST_MIPMAP_NEAREST : GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
                nearest ? GL_NEAREST : GL_LINEAR);
    }

    public boolean isNearest() { return nearest; }

    @Override
    public void close() { glDeleteTextures(textureId); }
}

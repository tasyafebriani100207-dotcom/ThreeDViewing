package com.grafkom.viewing3d;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL20.*;

/** Compiles a vertex/fragment shader pair from src/main/resources/shaders. */
public final class ShaderProgram implements AutoCloseable {
    private final int programId;

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vertex = compile(GL_VERTEX_SHADER, readResource(vertexResource));
        int fragment = compile(GL_FRAGMENT_SHADER, readResource(fragmentResource));
        programId = glCreateProgram();
        glAttachShader(programId, vertex);
        glAttachShader(programId, fragment);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            glDeleteProgram(programId);
            glDeleteShader(vertex);
            glDeleteShader(fragment);
            throw new IllegalStateException("Shader link failed:\n" + log);
        }
        glDetachShader(programId, vertex);
        glDetachShader(programId, fragment);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    public void use() { glUseProgram(programId); }

    public void setInt(String name, int value) {
        glUniform1i(glGetUniformLocation(programId, name), value);
    }

    public void setFloat(String name, float value) {
        glUniform1f(glGetUniformLocation(programId, name), value);
    }

    public void setBoolean(String name, boolean value) {
        setInt(name, value ? 1 : 0);
    }

    public void setVector3(String name, Vector3f value) {
        glUniform3f(glGetUniformLocation(programId, name), value.x, value.y, value.z);
    }

    public void setMatrix4(String name, Matrix4f value) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        value.get(buffer);
        glUniformMatrix4fv(glGetUniformLocation(programId, name), false, buffer);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed:\n" + log);
        }
        return shader;
    }

    private static String readResource(String path) {
        try (InputStream stream = ShaderProgram.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing classpath resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read shader: " + path, ex);
        }
    }

    @Override
    public void close() { glDeleteProgram(programId); }
}

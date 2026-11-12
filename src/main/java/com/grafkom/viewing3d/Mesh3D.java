package com.grafkom.viewing3d;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/** GPU-backed UV sphere and ground plane. Every vertex is position, normal, UV. */
public final class Mesh3D implements AutoCloseable {
    private int sphereVao;
    private int sphereVbo;
    private int sphereVertexCount;
    private int planeVao;
    private int planeVbo;
    private int planeVertexCount;

    public Mesh3D() {
        createSphere(48, 32);
        createGroundPlane(12.0f);
    }

    public void drawSphere() {
        glBindVertexArray(sphereVao);
        glDrawArrays(GL_TRIANGLES, 0, sphereVertexCount);
        glBindVertexArray(0);
    }

    public void drawGroundPlane() {
        glBindVertexArray(planeVao);
        glDrawArrays(GL_TRIANGLES, 0, planeVertexCount);
        glBindVertexArray(0);
    }

    private void createSphere(int slices, int stacks) {
        FloatBuilder data = new FloatBuilder();
        for (int row = 0; row < stacks; row++) {
            float theta0 = (float) (Math.PI * row / stacks);
            float theta1 = (float) (Math.PI * (row + 1) / stacks);
            for (int col = 0; col < slices; col++) {
                float phi0 = (float) (2.0 * Math.PI * col / slices);
                float phi1 = (float) (2.0 * Math.PI * (col + 1) / slices);

                float[] p00 = spherePoint(theta0, phi0);
                float[] p01 = spherePoint(theta0, phi1);
                float[] p10 = spherePoint(theta1, phi0);
                float[] p11 = spherePoint(theta1, phi1);

                // Counter-clockwise outward winding.
                addSphereVertex(data, p00, col / (float) slices, row / (float) stacks);
                addSphereVertex(data, p01, (col + 1) / (float) slices, row / (float) stacks);
                addSphereVertex(data, p10, col / (float) slices, (row + 1) / (float) stacks);

                addSphereVertex(data, p01, (col + 1) / (float) slices, row / (float) stacks);
                addSphereVertex(data, p11, (col + 1) / (float) slices, (row + 1) / (float) stacks);
                addSphereVertex(data, p10, col / (float) slices, (row + 1) / (float) stacks);
            }
        }
        sphereVertexCount = data.size() / 8;
        int[] gpu = upload(data.toArray());
        sphereVao = gpu[0];
        sphereVbo = gpu[1];
    }

    private static float[] spherePoint(float theta, float phi) {
        float sinTheta = (float) Math.sin(theta);
        return new float[]{
                sinTheta * (float) Math.cos(phi),
                (float) Math.cos(theta),
                sinTheta * (float) Math.sin(phi)
        };
    }

    private static void addSphereVertex(FloatBuilder data, float[] p, float u, float v) {
        data.add(p[0], p[1], p[2], p[0], p[1], p[2], u, v);
    }

    private void createGroundPlane(float halfSize) {
        FloatBuilder data = new FloatBuilder();
        float[] a = {-halfSize, 0, -halfSize};
        float[] b = { halfSize, 0, -halfSize};
        float[] c = { halfSize, 0,  halfSize};
        float[] d = {-halfSize, 0,  halfSize};
        // Up-facing triangles, UVs repeat to make filtering easy to compare.
        addPlaneVertex(data, a, 0, 0);
        addPlaneVertex(data, d, 0, 6);
        addPlaneVertex(data, c, 6, 6);
        addPlaneVertex(data, a, 0, 0);
        addPlaneVertex(data, c, 6, 6);
        addPlaneVertex(data, b, 6, 0);
        planeVertexCount = data.size() / 8;
        int[] gpu = upload(data.toArray());
        planeVao = gpu[0];
        planeVbo = gpu[1];
    }

    private static void addPlaneVertex(FloatBuilder data, float[] p, float u, float v) {
        data.add(p[0], p[1], p[2], 0, 1, 0, u, v);
    }

    private static int[] upload(float[] vertices) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    @Override
    public void close() {
        if (sphereVbo != 0) glDeleteBuffers(sphereVbo);
        if (sphereVao != 0) glDeleteVertexArrays(sphereVao);
        if (planeVbo != 0) glDeleteBuffers(planeVbo);
        if (planeVao != 0) glDeleteVertexArrays(planeVao);
    }

    private static final class FloatBuilder {
        private float[] values = new float[4096];
        private int size;
        void add(float... row) {
            if (size + row.length > values.length) {
                float[] grown = new float[Math.max(values.length * 2, size + row.length)];
                System.arraycopy(values, 0, grown, 0, size);
                values = grown;
            }
            System.arraycopy(row, 0, values, size, row.length);
            size += row.length;
        }
        int size() { return size; }
        float[] toArray() {
            float[] result = new float[size];
            System.arraycopy(values, 0, result, 0, size);
            return result;
        }
    }
}

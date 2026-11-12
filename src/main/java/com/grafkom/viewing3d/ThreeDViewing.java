package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;

/** Interactive OpenGL 3.3 visual realism demo. */
public class ThreeDViewing {
    private long window;
    private final Camera3D camera = new Camera3D();
    private final PerspectiveProjection perspective = new PerspectiveProjection();
    private final ProjectionTaxonomyManager projection = new ProjectionTaxonomyManager();
    private final StereoAnaglyphRenderer stereo = new StereoAnaglyphRenderer();

    private Mesh3D mesh;
    private ShaderProgram lightingShader;
    private ShaderProgram depthShader;
    private Texture2D surfaceTexture;
    private ShadowMap shadowMap;

    private boolean blinnPhong;
    private boolean smoothShading = true;
    private boolean depthTestEnabled = true;
    private boolean cullingEnabled;
    private boolean textureEnabled = true;
    private boolean nearestFiltering;
    private boolean shadowsEnabled = true;
    private boolean stereoEnabled;
    private boolean mousePressed;

    private double lastX;
    private double lastY;
    private int width = 1000;
    private int height = 700;
    private double fpsWindowStart;
    private int framesInFpsWindow;

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
            glfwTerminate();
        }
    }

    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height,
                "3D Visual Realism Suite", 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Failed to create OpenGL 3.3 window");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        glViewport(0, 0, width, height);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glClearColor(0.075f, 0.085f, 0.12f, 1.0f);

        mesh = new Mesh3D();
        lightingShader = new ShaderProgram(
                "/shaders/realism.vert", "/shaders/realism.frag");
        depthShader = new ShaderProgram(
                "/shaders/shadow_depth.vert", "/shaders/shadow_depth.frag");
        surfaceTexture = new Texture2D("/textures/checker.png");
        shadowMap = new ShadowMap();

        lightingShader.use();
        lightingShader.setInt("surfaceTexture", 0);
        lightingShader.setInt("shadowMap", 1);
        installCallbacks();

        System.out.println("Controls: B Phong/Blinn-Phong | F Flat/Smooth | "
                + "Z Depth Test | C Culling | T Texture | M Filter | H Shadows | "
                + "S Stereo | 1-6 Projections | Mouse orbit/zoom");
        fpsWindowStart = glfwGetTime();
    }

    private void installCallbacks() {
        glfwSetFramebufferSizeCallback(window, (w, newWidth, newHeight) -> {
            width = Math.max(newWidth, 1);
            height = Math.max(newHeight, 1);
            glViewport(0, 0, width, height);
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                mousePressed = action == GLFW_PRESS;
                if (mousePressed) {
                    double[] x = new double[1];
                    double[] y = new double[1];
                    glfwGetCursorPos(window, x, y);
                    lastX = x[0];
                    lastY = y[0];
                }
            }
        });

        glfwSetCursorPosCallback(window, (w, xpos, ypos) -> {
            if (mousePressed) {
                camera.rotate((float) (xpos - lastX), (float) (ypos - lastY));
                lastX = xpos;
                lastY = ypos;
            }
        });

        glfwSetScrollCallback(window,
                (w, xoffset, yoffset) -> camera.zoom((float) yoffset * 0.4f));

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS) return;
            switch (key) {
                case GLFW_KEY_ESCAPE:
                    glfwSetWindowShouldClose(window, true);
                    break;
                case GLFW_KEY_1:
                    projection.setProjection(ProjectionTaxonomyManager.ProjectionType.ORTHOGRAPHIC);
                    break;
                case GLFW_KEY_2:
                    projection.setProjection(ProjectionTaxonomyManager.ProjectionType.ISOMETRIC);
                    break;
                case GLFW_KEY_3:
                    projection.setProjection(ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER);
                    break;
                case GLFW_KEY_4:
                    projection.setProjection(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_1POINT);
                    break;
                case GLFW_KEY_5:
                    projection.setProjection(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_2POINT);
                    break;
                case GLFW_KEY_6:
                    projection.setProjection(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3POINT);
                    break;
                case GLFW_KEY_B:
                    blinnPhong = !blinnPhong;
                    report("Shading model", blinnPhong ? "Blinn-Phong" : "Phong");
                    break;
                case GLFW_KEY_F:
                    smoothShading = !smoothShading;
                    report("Surface shading", smoothShading ? "Smooth" : "Flat");
                    break;
                case GLFW_KEY_Z:
                    depthTestEnabled = !depthTestEnabled;
                    report("Z-Buffer depth test", onOff(depthTestEnabled));
                    break;
                case GLFW_KEY_C:
                    cullingEnabled = !cullingEnabled;
                    report("Back-face culling", onOff(cullingEnabled));
                    break;
                case GLFW_KEY_T:
                    textureEnabled = !textureEnabled;
                    report("Texture mapping", onOff(textureEnabled));
                    break;
                case GLFW_KEY_M:
                    nearestFiltering = !nearestFiltering;
                    surfaceTexture.setNearest(nearestFiltering);
                    report("Texture filter", nearestFiltering ? "Nearest" : "Bilinear mipmapped");
                    break;
                case GLFW_KEY_H:
                    shadowsEnabled = !shadowsEnabled;
                    report("Shadow mapping", onOff(shadowsEnabled));
                    break;
                case GLFW_KEY_S:
                    stereoEnabled = !stereoEnabled;
                    report("Stereo anaglyph", onOff(stereoEnabled));
                    break;
                case GLFW_KEY_EQUAL:
                case GLFW_KEY_KP_ADD:
                    perspective.setFovDegree(perspective.getFovDegree() - 5.0f);
                    report("Field of view", perspective.getFovDegree() + " degrees");
                    break;
                case GLFW_KEY_MINUS:
                case GLFW_KEY_KP_SUBTRACT:
                    perspective.setFovDegree(perspective.getFovDegree() + 5.0f);
                    report("Field of view", perspective.getFovDegree() + " degrees");
                    break;
                default:
                    break;
            }
        });
    }

    private void loop() {
        Vector3f lightPosition = new Vector3f(-3.5f, 6.0f, 3.0f);
        Matrix4f lightProjection = new Matrix4f().ortho(-7, 7, -7, 7, 0.5f, 20.0f);
        Matrix4f lightView = new Matrix4f().lookAt(
                lightPosition, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        Matrix4f lightSpace = lightProjection.mul(lightView, new Matrix4f());

        while (!glfwWindowShouldClose(window)) {
            renderShadowPass(lightSpace);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            float aspect = (float) width / height;
            Matrix4f projectionMatrix = makeProjection(aspect);
            Matrix4f view = camera.calculateLookAtMatrix();

            if (stereoEnabled) {
                glColorMask(true, false, false, true);
                renderScene(view, projectionMatrix, lightSpace, lightPosition);
                glClear(GL_DEPTH_BUFFER_BIT);
                glColorMask(false, true, true, true);
                Matrix4f rightEyeView = new Matrix4f(view).translate(stereo.getIpd(), 0, 0);
                renderScene(rightEyeView, projectionMatrix, lightSpace, lightPosition);
                glColorMask(true, true, true, true);
            } else {
                renderScene(view, projectionMatrix, lightSpace, lightPosition);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
            framesInFpsWindow++;
            double now = glfwGetTime();
            if (now - fpsWindowStart >= 1.0) {
                glfwSetWindowTitle(window, String.format(
                        "3D Visual Realism Suite | FPS: %d | %s | %s | Shadows %s",
                        framesInFpsWindow,
                        blinnPhong ? "Blinn-Phong" : "Phong",
                        smoothShading ? "Smooth" : "Flat",
                        onOff(shadowsEnabled)));
                framesInFpsWindow = 0;
                fpsWindowStart = now;
            }
        }
    }

    private void renderShadowPass(Matrix4f lightSpace) {
        // The shadow depth pass always needs depth testing, even when Z is
        // disabled for the visible camera pass to demonstrate its effect.
        boolean depthWasEnabled = glIsEnabled(GL_DEPTH_TEST);
        glEnable(GL_DEPTH_TEST);
        shadowMap.bindForWriting();
        glClear(GL_DEPTH_BUFFER_BIT);
        if (cullingEnabled) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);

        depthShader.use();
        depthShader.setMatrix4("lightSpaceMatrix", lightSpace);
        Matrix4f sphereModel = sphereModelMatrix();
        depthShader.setMatrix4("model", sphereModel);
        mesh.drawSphere();
        depthShader.setMatrix4("model", new Matrix4f());
        mesh.drawGroundPlane();

        shadowMap.unbind(width, height);
        if (depthWasEnabled) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
    }

    private void renderScene(Matrix4f view, Matrix4f projectionMatrix,
            Matrix4f lightSpace, Vector3f lightPosition) {
        if (depthTestEnabled) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        if (cullingEnabled) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);

        lightingShader.use();
        lightingShader.setMatrix4("view", view);
        lightingShader.setMatrix4("projection", projectionMatrix);
        lightingShader.setMatrix4("lightSpaceMatrix", lightSpace);
        lightingShader.setVector3("lightPos", lightPosition);
        lightingShader.setVector3("viewPos", camera.getEye());
        lightingShader.setVector3("lightColor", new Vector3f(1.0f, 0.96f, 0.88f));
        lightingShader.setBoolean("useBlinnPhong", blinnPhong);
        lightingShader.setBoolean("flatShading", !smoothShading);
        lightingShader.setBoolean("shadowsEnabled", shadowsEnabled);
        lightingShader.setBoolean("useTexture", textureEnabled);
        surfaceTexture.bind(0);
        shadowMap.bindForReading(1);

        lightingShader.setMatrix4("model", sphereModelMatrix());
        lightingShader.setVector3("objectColor", new Vector3f(0.95f, 0.88f, 0.78f));
        mesh.drawSphere();

        lightingShader.setMatrix4("model", new Matrix4f());
        lightingShader.setVector3("objectColor", new Vector3f(0.72f, 0.74f, 0.78f));
        mesh.drawGroundPlane();
    }

    private Matrix4f sphereModelMatrix() {
        Matrix4f model = new Matrix4f().translate(0, 1.0f, 0);
        switch (projection.getCurrentType()) {
            case ISOMETRIC:
                model.rotateX((float) Math.toRadians(35.264f));
                model.rotateY((float) Math.toRadians(45.0f));
                break;
            case OBLIQUE_CAVALIER:
                model.rotateY((float) Math.toRadians(30.0f));
                break;
            case PERSPECTIVE_2POINT:
                model.rotateY((float) Math.toRadians(20.0f));
                break;
            case PERSPECTIVE_3POINT:
                model.rotateY((float) Math.toRadians(20.0f));
                model.rotateX((float) Math.toRadians(15.0f));
                break;
            default:
                break;
        }
        return model;
    }

    private Matrix4f makeProjection(float aspect) {
        switch (projection.getCurrentType()) {
            case ORTHOGRAPHIC:
            case ISOMETRIC:
            case OBLIQUE_CAVALIER:
                return new Matrix4f().ortho(-3.5f * aspect, 3.5f * aspect,
                        -3.5f, 3.5f, 0.1f, 50.0f);
            default:
                perspective.setAspectRatio(aspect);
                return perspective.calculateProjectionMatrix();
        }
    }

    private static String onOff(boolean enabled) { return enabled ? "ON" : "OFF"; }

    private static void report(String feature, String value) {
        System.out.println(feature + ": " + value);
    }

    private void cleanup() {
        if (window != 0) {
            if (mesh != null) mesh.close();
            if (lightingShader != null) lightingShader.close();
            if (depthShader != null) depthShader.close();
            if (surfaceTexture != null) surfaceTexture.close();
            if (shadowMap != null) shadowMap.close();
            glfwDestroyWindow(window);
            window = 0;
        }
    }

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }
}

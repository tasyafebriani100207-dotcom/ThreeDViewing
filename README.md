# 3D Visual Realism Suite

This Maven project extends the ThreeDViewing viewer with an OpenGL 3.3 core rendering path. It demonstrates Phong and Blinn-Phong lighting, flat and smooth shading, depth testing, back-face culling, UV texturing with mipmapped filtering, and two-pass shadow mapping onto a ground plane.

## Run

Open the `ThreeDViewing` folder as a Maven project in NetBeans and run `com.grafkom.viewing3d.ThreeDViewing`. The application needs a graphics driver that supports OpenGL 3.3. The supplied Maven natives target Windows.

## Controls

| Key | Feature |
| --- | --- |
| B | Switch Phong / Blinn-Phong |
| F | Switch flat / smooth surface shading |
| Z | Switch camera depth testing |
| C | Switch back-face culling |
| T | Show / hide the UV texture |
| M | Switch nearest / bilinear mipmapped filtering |
| H | Switch shadow mapping |
| S | Switch stereo anaglyph |
| 1–6 | Switch projection modes |
| Left mouse drag | Orbit camera |
| Mouse wheel | Zoom |
| + / - | Change perspective field of view |
| Escape | Close the viewer |

The window title reports the approximate FPS over one-second intervals. The shadow pass uses a 2048×2048 depth texture and a slope-dependent depth bias with 3×3 percentage-closer filtering.

## Practical report checklist

Capture the viewer and the Output window for each required comparison: Phong vs Blinn-Phong, flat vs smooth, depth/culling on and off, texture filtering modes, and shadows on and off. Record the FPS shown in the title for the same camera and scene settings. Explain that nearest filtering preserves hard texel edges, bilinear mipmapped filtering smooths minification, and increasing shadow bias trades shadow acne for peter-panning. Include the five shader and visibility features, the sphere UV mesh, and the shadow depth framebuffer in the implementation discussion.

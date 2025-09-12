package io.github.simulation.gl;

import com.badlogic.gdx.Gdx;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Utility class for checking OpenGL capabilities required by the simulation
 */
public class GLCapabilitiesChecker {

    private boolean computeShaderSupported = false;

    public boolean checkCapabilities(boolean logDetails) {
        GLCapabilities caps = GL.getCapabilities();

        if (logDetails) {
            Gdx.app.log("GL", "GL_VERSION: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));
            Gdx.app.log("GL", "GLSL VERSION: "
                    + org.lwjgl.opengl.GL20.glGetString(org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION));
            Gdx.app.log("GL", "caps.OpenGL43: " + caps.OpenGL43);
            Gdx.app.log("GL", "caps.GL_ARB_compute_shader: " + caps.GL_ARB_compute_shader);

            
        }

        computeShaderSupported = caps.OpenGL43 || caps.GL_ARB_compute_shader;

        if (!computeShaderSupported) {
            Gdx.app.error("Compute", "No compute shader / SSBO support.");
            return false;
        }

        return true;
    }

    public boolean isComputeShaderSupported() {
        return computeShaderSupported;
    }
}

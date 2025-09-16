package io.github.simulation.gl;

import org.lwjgl.opengl.*;

/**
 * Utility class for managing OpenGL state to prevent interference between
 * different rendering systems
 */
public class GLStateManager {

    /**
     * Ensures that OpenGL state is properly set up for compute shader and
     * instanced rendering
     */
    public static void ensureComputeRenderState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL33.GL_PROGRAM_POINT_SIZE);

        // Enable point sprites because of Compatibility Profile
        GL31.glEnable(GL31.GL_POINT_SPRITE);
        GL31.glPointParameteri(GL20.GL_POINT_SPRITE_COORD_ORIGIN, GL20.GL_LOWER_LEFT);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL20.glUseProgram(0);
    }

    /**
     * Particle render pass state 
     */
    public static void ensureParticleRenderState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL33.GL_PROGRAM_POINT_SIZE);

        GL31.glEnable(GL31.GL_POINT_SPRITE);
        GL31.glPointParameteri(GL20.GL_POINT_SPRITE_COORD_ORIGIN, GL20.GL_LOWER_LEFT);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL20.glUseProgram(0);
    }

    /**
     * Blit/composite pass state 
     */
    public static void ensureBlitState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        // Do not unbind texture here; caller will bind the FBO texture

        GL20.glUseProgram(0);
    }

    /**
     * Sets up OpenGL state for SpriteBatch compatibility
     */
    public static void ensureSpriteBatchCompatibility() {
        GL20.glUseProgram(0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }
}
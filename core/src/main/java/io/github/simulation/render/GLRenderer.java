package io.github.simulation.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import io.github.simulation.config.SimulationConfig;
import io.github.simulation.gl.GLStateManager;

import java.nio.FloatBuffer;

/**
 * Handles instanced GPU rendering of particles
 */
public class GLRenderer {

    private final int renderProgram;
    private int vao = 0;
    private int vbo = 0;

    public GLRenderer(int renderProgram) {
        this.renderProgram = renderProgram;
    }

    public boolean initialize() {
        setupOpenGLState();
        setupVertexArrayObject();
        return true;
    }

    public void render() {
        // Clear screen
        ScreenUtils.clear(SimulationConfig.BACKGROUND_COLOR[0], SimulationConfig.BACKGROUND_COLOR[1],
                SimulationConfig.BACKGROUND_COLOR[2], SimulationConfig.BACKGROUND_COLOR[3]);

        GLStateManager.ensureComputeRenderState();
        GL20.glUseProgram(renderProgram);

        // Set screen dimensions uniform
        setUniform("u_PointSize", SimulationConfig.PARTICLE_SIZE_PX);

        // Draw instanced points
        GL30.glBindVertexArray(vao);
        GL31.glDrawArraysInstanced(GL11.GL_POINTS, 0, 1, SimulationConfig.PARTICLE_COUNT);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(0);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            Gdx.app.error("GLRenderer", "Error after draw: 0x" + Integer.toHexString(error));
        }
    }

    private void setupOpenGLState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL33.GL_PROGRAM_POINT_SIZE);

        // Enable point sprites because of Compatibility Profile
        GL31.glEnable(GL31.GL_POINT_SPRITE);
        GL31.glPointParameteri(GL20.GL_POINT_SPRITE_COORD_ORIGIN, GL20.GL_LOWER_LEFT);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void setupVertexArrayObject() {
        // Create VAO/VBO for single vertex instanced rendering
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(3);
        vertexBuffer.put(0f).put(0f).put(0f).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void setUniform(String name, float value) {
        int location = GL20.glGetUniformLocation(renderProgram, name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    public void dispose() {
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
            vao = 0;
        }
    }
}

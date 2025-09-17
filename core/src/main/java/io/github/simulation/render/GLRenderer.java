package io.github.simulation.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import io.github.simulation.config.RuntimeConfig;
import io.github.simulation.config.SimulationConfig;
import io.github.simulation.gl.GLStateManager;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Handles instanced GPU rendering of particles
 */
public class GLRenderer {

    private final int renderProgram;
    private final int blitProgram;

    // Particle rendering VAO/VBO 
    private int vao = 0;
    private int vbo = 0;

    // Offscreen square framebuffer (H x H)
    private int fbo = 0;
    private int fboTex = 0;
    private int fboRbo = 0;
    private int fboSize = 0; // equals current window height

    // Blit resources 
    private int blitVao = 0;
    private int blitVbo = 0;

    public GLRenderer(int renderProgram, int blitProgram) {
        this.renderProgram = renderProgram;
        this.blitProgram = blitProgram;
    }

    public boolean initialize() {
        setupParticleVAO();   // for instanced point rendering
        setupBlitQuad();      // fullscreen quad for compositing
        recreateOffscreenIfNeeded();
        return true;
    }

    public void render() {
        int W = Gdx.graphics.getWidth();
        int H = Gdx.graphics.getHeight();
        int barWidth = Math.max(0, (W - H) / 2);
        int squareSize = H;

        // (Re)create FBO when size changes
        if (squareSize != fboSize || fbo == 0 || fboTex == 0) {
            recreateOffscreenIfNeeded();
        }

        // Pass 1: render particles into square offscreen FBO
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, squareSize, squareSize);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        ScreenUtils.clear(SimulationConfig.BACKGROUND_COLOR[0], SimulationConfig.BACKGROUND_COLOR[1],
                SimulationConfig.BACKGROUND_COLOR[2], SimulationConfig.BACKGROUND_COLOR[3]);

        GLStateManager.ensureParticleRenderState();
        GL20.glUseProgram(renderProgram);

        // For square offscreen we do not need horizontal squeeze
        setUniform1f(renderProgram, "u_AspectScale", 1.0f);
        setUniform1f(renderProgram, "u_PointSize", SimulationConfig.PARTICLE_SIZE_PX);

        GL30.glBindVertexArray(vao);
        GL31.glDrawArraysInstanced(GL11.GL_POINTS, 0, 1, RuntimeConfig.getParticleCount());
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);

        // Pass 2: composite to default framebuffer: center + mirrored side bars
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, W, H);
        ScreenUtils.clear(SimulationConfig.BACKGROUND_COLOR[0], SimulationConfig.BACKGROUND_COLOR[1],
                SimulationConfig.BACKGROUND_COLOR[2], SimulationConfig.BACKGROUND_COLOR[3]);

        GLStateManager.ensureBlitState();

        GL20.glUseProgram(blitProgram);
        // Bind offscreen texture to unit 0
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTex);
        setUniform1i(blitProgram, "u_Texture", 0);

        GL30.glBindVertexArray(blitVao);

        float ratio = (squareSize > 0) ? (barWidth / (float) squareSize) : 0f;

        // Draw center square
        if (barWidth >= 0) {
            GL11.glViewport(barWidth, 0, squareSize, squareSize);
            setUniform1i(blitProgram, "u_ForceWhite", 0);
            setUniform4f(blitProgram, "u_UVRect", 0f, 0f, 1f, 1f); // full texture
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        }

        // Left bar
        if (barWidth > 0) {
            GL11.glViewport(0, 0, barWidth, squareSize);
            setUniform1i(blitProgram, "u_ForceWhite", 1);
            setUniform4f(blitProgram, "u_UVRect", 1f - ratio, 0f, ratio, 1f);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        }

        // Right bar
        if (barWidth > 0) {
            GL11.glViewport(barWidth + squareSize, 0, barWidth, squareSize);
            setUniform1i(blitProgram, "u_ForceWhite", 1);
            setUniform4f(blitProgram, "u_UVRect", 0f, 0f, ratio, 1f);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        }

        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);

        // Restore full viewport for anything that renders afterwards 
        GL11.glViewport(0, 0, W, H);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            Gdx.app.error("GLRenderer", "Error after draw: 0x" + Integer.toHexString(error));
        }
    }

    private void setupParticleVAO() {
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

    private void setupBlitQuad() {
        // Fullscreen quad, with UVs
        float[] quad = new float[]{
                // x,  y,   u, v
                -1f, -1f,  0f, 0f,
                 1f, -1f,  1f, 0f,
                 1f,  1f,  1f, 1f,
                -1f, -1f,  0f, 0f,
                 1f,  1f,  1f, 1f,
                -1f,  1f,  0f, 1f
        };

        blitVao = GL30.glGenVertexArrays();
        blitVbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(blitVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, blitVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);

        int stride = (2 + 2) * Float.BYTES;
        GL20.glEnableVertexAttribArray(0); // position
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1); // uv
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2 * Float.BYTES);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void recreateOffscreenIfNeeded() {
        int H = Gdx.graphics.getHeight();
        int newSize = Math.max(1, H);

        if (fbo != 0 && newSize == fboSize) return;

        // Dispose old
        if (fbo != 0) {
            GL30.glDeleteFramebuffers(fbo);
            fbo = 0;
        }
        if (fboTex != 0) {
            GL11.glDeleteTextures(fboTex);
            fboTex = 0;
        }
        if (fboRbo != 0) {
            GL30.glDeleteRenderbuffers(fboRbo);
            fboRbo = 0;
        }

        fboSize = newSize;

        // Create color texture
        fboTex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, fboSize, fboSize, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (IntBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // Create renderbuffer for depth 
        fboRbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, fboRbo);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, fboSize, fboSize);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);

        // Create framebuffer
        fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, fboTex, 0);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, fboRbo);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            Gdx.app.error("GLRenderer", "FBO incomplete: 0x" + Integer.toHexString(status));
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private void setUniform1f(int program, String name, float value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private void setUniform1i(int program, String name, int value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    private void setUniform4f(int program, String name, float x, float y, float z, float w) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform4f(location, x, y, z, w);
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
        if (blitVbo != 0) {
            GL15.glDeleteBuffers(blitVbo);
            blitVbo = 0;
        }
        if (blitVao != 0) {
            GL30.glDeleteVertexArrays(blitVao);
            blitVao = 0;
        }
        if (fbo != 0) {
            GL30.glDeleteFramebuffers(fbo);
            fbo = 0;
        }
        if (fboTex != 0) {
            GL11.glDeleteTextures(fboTex);
            fboTex = 0;
        }
        if (fboRbo != 0) {
            GL30.glDeleteRenderbuffers(fboRbo);
            fboRbo = 0;
        }
    }
}
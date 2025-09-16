package io.github.simulation.shader;

import com.badlogic.gdx.Gdx;
import org.lwjgl.opengl.*;
import io.github.simulation.config.RuntimeConfig;

/**
 * Manages shader compilation and program creation 
 */
public class ShaderManager {

    private int computeProgram = 0;
    private int renderProgram = 0;
    private int blitProgram = 0;

    public boolean initializeShaders() {
        // Particle render program
        String vertexShader = Gdx.files.internal("shaders/particle.vert").readString();
        String fragmentShader = Gdx.files.internal("shaders/particle.frag").readString();
        renderProgram = createRenderProgram(vertexShader, fragmentShader);

        // Compute program
        String computeShaderSource = Gdx.files.internal("shaders/particle.comp").readString();
        computeProgram = createComputeProgram(computeShaderSource);

        // Blit program (for compositing the offscreen square)
        String blitVert = Gdx.files.internal("shaders/blit.vert").readString();
        String blitFrag = Gdx.files.internal("shaders/blit.frag").readString();
        blitProgram = createProgram(blitVert, blitFrag);

        if (computeProgram == 0 || renderProgram == 0 || blitProgram == 0) {
            Gdx.app.error("ShaderManager", "Shaders failed to compile/link.");
            return false;
        }
        return true;
    }

    public boolean regenerateComputeShader() {
        return computeProgram != 0;
    }

    public void updateComputeUniforms() {
        GL20.glUseProgram(computeProgram);

        // Update runtime configuration uniforms
        setUniform(computeProgram, "u_force_factor", RuntimeConfig.getForceFactor());
        setUniform(computeProgram, "u_velocity_damping", RuntimeConfig.getVelocityDamping());
        setUniform(computeProgram, "u_interaction_range", RuntimeConfig.getInteractionRange());

        GL20.glUseProgram(0);
    }

    private void setUniform(int program, String name, float value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    public int getComputeProgram() {
        return computeProgram;
    }

    public int getRenderProgram() {
        return renderProgram;
    }

    public int getBlitProgram() {
        return blitProgram;
    }

    public void dispose() {
        if (computeProgram != 0) {
            GL20.glDeleteProgram(computeProgram);
            computeProgram = 0;
        }
        if (renderProgram != 0) {
            GL20.glDeleteProgram(renderProgram);
            renderProgram = 0;
        }
        if (blitProgram != 0) {
            GL20.glDeleteProgram(blitProgram);
            blitProgram = 0;
        }
    }

    private int createComputeProgram(String src) {
        int cs = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        GL20.glShaderSource(cs, src);
        GL20.glCompileShader(cs);

        if (GL20.glGetShaderi(cs, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(cs);
            Gdx.app.error("ShaderManager", "Compute shader compile error:\n" + log);
            GL20.glDeleteShader(cs);
            return 0;
        }

        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, cs);
        GL20.glLinkProgram(prog);

        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(prog);
            Gdx.app.error("ShaderManager", "Compute program link error:\n" + log);
            GL20.glDeleteProgram(prog);
            GL20.glDeleteShader(cs);
            return 0;
        }

        GL20.glDetachShader(prog, cs);
        GL20.glDeleteShader(cs);
        return prog;
    }

    // Particle render program 
    private int createRenderProgram(String vertSrc, String fragSrc) {
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vertSrc);
        GL20.glCompileShader(vs);

        if (GL20.glGetShaderi(vs, GL20.GL_COMPILE_STATUS) == 0) {
            Gdx.app.error("ShaderManager", "Vertex shader compile error:\n" + GL20.glGetShaderInfoLog(vs));
            GL20.glDeleteShader(vs);
            return 0;
        }

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, fragSrc);
        GL20.glCompileShader(fs);

        if (GL20.glGetShaderi(fs, GL20.GL_COMPILE_STATUS) == 0) {
            Gdx.app.error("ShaderManager", "Fragment shader compile error:\n" + GL20.glGetShaderInfoLog(fs));
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            return 0;
        }

        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glBindAttribLocation(prog, 0, "in_pos");
        GL20.glLinkProgram(prog);

        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == 0) {
            Gdx.app.error("ShaderManager", "Render program link error:\n" + GL20.glGetProgramInfoLog(prog));
            GL20.glDeleteProgram(prog);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            return 0;
        }

        GL20.glDetachShader(prog, vs);
        GL20.glDetachShader(prog, fs);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        return prog;
    }

    // Generic program builder 
    private int createProgram(String vertSrc, String fragSrc) {
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vertSrc);
        GL20.glCompileShader(vs);
        if (GL20.glGetShaderi(vs, GL20.GL_COMPILE_STATUS) == 0) {
            Gdx.app.error("ShaderManager", "Vertex shader compile error:\n" + GL20.glGetShaderInfoLog(vs));
            GL20.glDeleteShader(vs);
            return 0;
        }

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, fragSrc);
        GL20.glCompileShader(fs);
        if (GL20.glGetShaderi(fs, GL20.GL_COMPILE_STATUS) == 0) {
            Gdx.app.error("ShaderManager", "Fragment shader compile error:\n" + GL20.glGetShaderInfoLog(fs));
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            return 0;
        }

        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glLinkProgram(prog);

        GL20.glDetachShader(prog, vs);
        GL20.glDetachShader(prog, fs);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == 0) {
            Gdx.app.error("ShaderManager", "Program link error:\n" + GL20.glGetProgramInfoLog(prog));
            GL20.glDeleteProgram(prog);
            return 0;
        }
        return prog;
    }
}
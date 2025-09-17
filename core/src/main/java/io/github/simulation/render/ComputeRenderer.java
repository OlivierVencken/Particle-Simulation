package io.github.simulation.render;

import com.badlogic.gdx.Gdx;
import org.lwjgl.opengl.*;
import io.github.simulation.config.SimulationConfig;
import io.github.simulation.config.RuntimeConfig;
import io.github.simulation.config.RuntimeGrid;

/**
 * Handles compute shader execution for particle physics
 */
public class ComputeRenderer {

    private final int computeProgram;

    public ComputeRenderer(int computeProgram) {
        this.computeProgram = computeProgram;
    }

    public void executeComputeShader(float deltaTime, float totalTime) {
        GL20.glUseProgram(computeProgram);

        // Apply time scaling to delta time
        float scaledDeltaTime = deltaTime * RuntimeConfig.getTimeScale();

        // Set basic uniforms
        setUniform("u_dt", scaledDeltaTime);
        setUniform("u_count", RuntimeConfig.getParticleCount());
        setUniform("u_group_count", RuntimeConfig.getGroupCount());

        // Update runtime configuration uniforms
        setUniform("u_force_factor", RuntimeConfig.getForceFactor());
        setUniform("u_velocity_damping", RuntimeConfig.getVelocityDamping());
        setUniform("u_interaction_range", RuntimeConfig.getInteractionRange());
        setUniform("u_grid_size", RuntimeGrid.getGridSize());
        setUniform("u_grid_cell_size", RuntimeGrid.getGridCellSize());
        setUniform("u_max_particles_per_cell", RuntimeGrid.getMaxParticlesPerCell());

        updateAttractionMatrix();

        // skip if no particles
        int particleCount = RuntimeConfig.getParticleCount();
        if (particleCount == 0) {
            // Nothing to process; avoid invalid dispatch (0 workgroups)
            GL20.glUseProgram(0);
            return;
        }

        // Calculate dispatch groups
        int groups = (particleCount + SimulationConfig.WORKGROUP_SIZE - 1)
                / SimulationConfig.WORKGROUP_SIZE;

        // Populate spatial grid
        setUniform("u_pass", 0);
        GL43.glDispatchCompute(groups, 1, 1);
        GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);

        // Calculate forces using spatial grid
        setUniform("u_pass", 1);
        GL43.glDispatchCompute(groups, 1, 1);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            Gdx.app.error("ComputeRenderer", "Error after glDispatchCompute: 0x" + Integer.toHexString(error));
        }

        GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        GL20.glUseProgram(0);
    }

    private void updateAttractionMatrix() {
        int g = RuntimeConfig.getGroupCount();
        float[][] m = RuntimeConfig.getAttractionMatrix();

        int loc = GL20.glGetUniformLocation(computeProgram, "u_attraction_matrix");
        if (loc < 0) {
            return; // uniform not found
        }

        int max = SimulationConfig.MAX_GROUPS * SimulationConfig.MAX_GROUPS;
        float[] flat = new float[max];
        int idx = 0;
        for (int r = 0; r < g; r++) {
            for (int c = 0; c < g; c++) {
                flat[idx++] = m[r][c];
            }
        }
        // rest already zero
        GL20.glUniform1fv(loc, flat);
    }

    private void setUniform(String name, float value) {
        int location = GL20.glGetUniformLocation(computeProgram, name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private void setUniform(String name, int value) {
        int location = GL20.glGetUniformLocation(computeProgram, name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }
}

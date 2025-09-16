package io.github.simulation.particles;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import io.github.simulation.config.SimulationConfig;
import io.github.simulation.config.RuntimeGrid;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Manages particle data and SSBO operations
 */
public class ParticleSystem {

    private int particleSSBO = 0;
    private int gridDataSSBO = 0;
    private int gridCountsSSBO = 0;

    public boolean initialize() {
        // Create particle SSBO
        particleSSBO = GL15.glGenBuffers();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSSBO);

        FloatBuffer initial = createInitialParticleData();

        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, initial, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, particleSSBO);

        // Create spatial grid buffers
        createGridBuffers();

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        return true;
    }

    public void checkAndRebuildGrid() {
        RuntimeGrid.updateGridParameters();
        if (RuntimeGrid.needsGridRebuild()) {
            rebuildGridBuffers();
            RuntimeGrid.markGridRebuilt();
        }
    }

    private void rebuildGridBuffers() {
        // Delete old grid buffers
        if (gridDataSSBO != 0) {
            GL15.glDeleteBuffers(gridDataSSBO);
        }
        if (gridCountsSSBO != 0) {
            GL15.glDeleteBuffers(gridCountsSSBO);
        }

        // Create new grid buffers with current parameters
        createGridBuffers();
    }

    private void createGridBuffers() {
        int totalCells = RuntimeGrid.getGridSize() * RuntimeGrid.getGridSize();
        int gridDataSize = totalCells * RuntimeGrid.getMaxParticlesPerCell();

        // Grid data buffer --> stores particle indices for each cell
        gridDataSSBO = GL15.glGenBuffers();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridDataSSBO);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, gridDataSize * Integer.BYTES, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, gridDataSSBO);

        // Grid counts buffer --> stores count of particles in each cell
        gridCountsSSBO = GL15.glGenBuffers();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridCountsSSBO);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, totalCells * Integer.BYTES, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, gridCountsSSBO);
    }

    public int getSSBO() {
        return particleSSBO;
    }

    public void bindSSBO() {
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSSBO);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, particleSSBO);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridDataSSBO);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, gridDataSSBO);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridCountsSSBO);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, gridCountsSSBO);
    }

    public void clearGrid() {
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridCountsSSBO);

        // Clear the buffer with zeros
        int totalCells = RuntimeGrid.getGridSize() * RuntimeGrid.getGridSize();
        IntBuffer zeros = BufferUtils.createIntBuffer(totalCells);
        for (int i = 0; i < totalCells; i++) {
            zeros.put(0);
        }
        zeros.flip();
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, zeros);
    }

    public void unbindSSBO() {
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void dispose() {
        if (particleSSBO != 0) {
            GL15.glDeleteBuffers(particleSSBO);
            particleSSBO = 0;
        }
        if (gridDataSSBO != 0) {
            GL15.glDeleteBuffers(gridDataSSBO);
            gridDataSSBO = 0;
        }
        if (gridCountsSSBO != 0) {
            GL15.glDeleteBuffers(gridCountsSSBO);
            gridCountsSSBO = 0;
        }
    }

    private FloatBuffer createInitialParticleData() {
        FloatBuffer initial = BufferUtils.createFloatBuffer(
                SimulationConfig.PARTICLE_COUNT * SimulationConfig.PARTICLE_STRIDE_FLOATS);

        for (int i = 0; i < SimulationConfig.PARTICLE_COUNT; i++) {
            // Position 
            float[] p = sampleCentralPosition();
            float px = p[0];
            float py = p[1];
            initial.put(px).put(py).put(0f).put(1f);

            // Velocity 
            float vx = 0.0f; 
            float vy = 0.0f;
            initial.put(vx).put(vy).put(0f).put(0f);

            // Assign particle to a group 
            int group = i % SimulationConfig.PARTICLE_GROUPS;
            float[] groupColor = SimulationConfig.GROUP_COLORS[group];

            // Color 
            initial.put(groupColor[0]).put(groupColor[1]).put(groupColor[2]).put(groupColor[3]);

            // Group/Type information --> store group index in x component
            initial.put((float) group).put(0f).put(0f).put(0f);
        }

        initial.flip();
        return initial;
    }

    /**
     * Uniform distribution in the square [-1, 1] x [-1, 1].
     * Matches the current behavior.
     */
    private static float[] sampleUniformPosition() {
        float x = (float) (Math.random() * 2.0 - 1.0);
        float y = (float) (Math.random() * 2.0 - 1.0);
        return new float[] { x, y };
    }

    /**
     * Center-biased distribution inside the unit disk.
     * Uses a radial power law r = U^beta with beta > 0.5 to favor small radii,
     * and a uniform angle. Increase CENTER_BIAS_BETA for stronger center density.
     */
    private static final float CENTER_BIAS_BETA = 2.0f; // > 0.5 biases toward center
    private static float[] sampleCentralPosition() {
        double u = Math.random();                 // [0,1)
        double theta = Math.random() * Math.PI * 2.0;
        double r = Math.pow(u, CENTER_BIAS_BETA); // radius in [0,1), biased small
        float x = (float) (r * Math.cos(theta));
        float y = (float) (r * Math.sin(theta));
        return new float[] { x, y };
    }
}

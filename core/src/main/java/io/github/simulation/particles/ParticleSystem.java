package io.github.simulation.particles;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import io.github.simulation.config.SimulationConfig;
import io.github.simulation.config.RuntimeConfig;
import io.github.simulation.config.RuntimeConfig.Distribution;
import io.github.simulation.config.RuntimeGrid;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages particle data and SSBO operations
 */
public class ParticleSystem {

    private int particleSSBO = 0;
    private int gridDataSSBO = 0;
    private int gridCountsSSBO = 0;
    private int particleCapacity = 0;

    public boolean initialize() {
        int startCount = RuntimeConfig.getParticleCount();
        createParticleBuffer(startCount);
        createGridBuffers();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        return true;
    }

    private void createParticleBuffer(int count) {
        if (particleSSBO != 0) {
            GL15.glDeleteBuffers(particleSSBO);
        }
        particleSSBO = GL15.glGenBuffers();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSSBO);

        long bytes = (long) count * SimulationConfig.PARTICLE_STRIDE_FLOATS * Float.BYTES;
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bytes, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, particleSSBO);

        if (count > 0) {
            FloatBuffer seed = createInitialParticleData(count, SimulationConfig.DISTRIBUTION);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, seed);
        }
        particleCapacity = count;
        RuntimeConfig.setParticleCount(count);
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

    public void addRandomParticles(int n) {
        if (n <= 0)
            return;
        int current = RuntimeConfig.getParticleCount();
        int needed = current + n;
        if (needed > particleCapacity) {
            growCapacity(Math.max(needed, particleCapacity * 2));
        }
        // Generate new particle data
        FloatBuffer data = createInitialParticleData(n, RuntimeConfig.getDistribution());
        long strideBytes = (long) SimulationConfig.PARTICLE_STRIDE_FLOATS * Float.BYTES;
        long dstOffset = (long) current * strideBytes;
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSSBO);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, dstOffset, data);
        RuntimeConfig.setParticleCount(needed);
    }

    public void removeRandomParticles(int n) {
        int current = RuntimeConfig.getParticleCount();
        if (n <= 0 || current == 0)
            return;
        n = Math.min(n, current);
        int newCount = current;

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSSBO);
        // READ_WRITE mapping
        ByteBuffer bb = GL15.glMapBuffer(GL43.GL_SHADER_STORAGE_BUFFER, GL15.GL_READ_WRITE);
        if (bb == null) {
            return; // mapping failed
        }
        FloatBuffer fb = bb.asFloatBuffer();
        int strideFloats = SimulationConfig.PARTICLE_STRIDE_FLOATS;

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int k = 0; k < n; k++) {
            if (newCount == 0)
                break;
            int removeIndex = rng.nextInt(newCount);
            int lastIndex = newCount - 1;
            if (removeIndex != lastIndex) {
                int baseA = removeIndex * strideFloats;
                int baseB = lastIndex * strideFloats;
                // Swap (move last into removeIndex)
                for (int i = 0; i < strideFloats; i++) {
                    float v = fb.get(baseB + i);
                    fb.put(baseA + i, v);
                }
            }
            newCount--;
        }
        GL15.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
        RuntimeConfig.setParticleCount(newCount);
    }

    public void reassignGroupsIfNeeded() {
        if (!RuntimeConfig.consumeGroupsChanged()) {
            return;
        }

        int current = RuntimeConfig.getParticleCount();
        if (current == 0) {
            return;
        }

        int gCount = RuntimeConfig.getGroupCount();
        float[][] palette = RuntimeConfig.getGroupColors();

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSSBO);
        ByteBuffer bb = GL15.glMapBuffer(GL43.GL_SHADER_STORAGE_BUFFER, GL15.GL_READ_WRITE);
        if (bb == null) {
            return;
        }
        FloatBuffer fb = bb.asFloatBuffer();

        int stride = SimulationConfig.PARTICLE_STRIDE_FLOATS;
        for (int i = 0; i < current; i++) {
            int base = i * stride;
            int groupId = i % gCount;

            // color offset
            int colBase = base + SimulationConfig.OFFSET_COLOR;
            float[] col = palette[groupId];
            fb.put(colBase, col[0]);
            fb.put(colBase + 1, col[1]);
            fb.put(colBase + 2, col[2]);
            fb.put(colBase + 3, col[3]);

            // meta/group id offset
            fb.put(base + SimulationConfig.OFFSET_META, (float) groupId);
        }

        GL15.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
    }

    // Grow capacity preserving existing particle data
    private void growCapacity(int newCapacity) {
        if (newCapacity <= particleCapacity) {
            return;
        }

        int currentCount = RuntimeConfig.getParticleCount();

        // Allocate new buffer sized exactly to newCapacity
        int newBuffer = GL15.glGenBuffers();
        long newBytes = (long) newCapacity * SimulationConfig.PARTICLE_STRIDE_FLOATS * Float.BYTES;
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, newBuffer);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_DRAW);

        // Copy existing particle data if any
        if (currentCount > 0) {
            long copyBytes = (long) currentCount * SimulationConfig.PARTICLE_STRIDE_FLOATS * Float.BYTES;
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, particleSSBO);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, newBuffer);
            GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, 0, 0, copyBytes);
        }

        // Bind new buffer as SSBO 0 and delete old
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, newBuffer);
        GL15.glDeleteBuffers(particleSSBO);

        particleSSBO = newBuffer;
        particleCapacity = newCapacity;
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

    public void bindSSBO() {
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSSBO);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, particleSSBO);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridDataSSBO);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, gridDataSSBO);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, gridCountsSSBO);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, gridCountsSSBO);
    }

    public int getSSBO() {
        return particleSSBO;
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

    private FloatBuffer createInitialParticleData(int count, Distribution dist) {
        FloatBuffer initial = BufferUtils.createFloatBuffer(count * SimulationConfig.PARTICLE_STRIDE_FLOATS);
        int groups = SimulationConfig.PARTICLE_GROUPS;
        for (int i = 0; i < count; i++) {
            // Assign a group
            int groupId = i % groups;

            // Position
            float[] p = samplePosition(dist);
            initial.put(p[0]).put(p[1]).put(0f).put(1f); // pos (w=1)

            // Velocity
            initial.put(0f).put(0f).put(0f).put(0f);

            // Color
            float[] col = SimulationConfig.GROUP_COLORS[groupId];
            initial.put(col[0]).put(col[1]).put(col[2]).put(col[3]);

            // x = group id
            initial.put((float) groupId).put(0f).put(0f).put(0f);
        }
        initial.flip();
        return initial;
    }

    public void repositionAllParticles(Distribution dist) {
        int count = RuntimeConfig.getParticleCount();
        if (count == 0) {
            return;
        }
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSSBO);
        ByteBuffer bb = GL15.glMapBuffer(GL43.GL_SHADER_STORAGE_BUFFER, GL15.GL_READ_WRITE);
        if (bb == null) {
            return;
        }
        FloatBuffer fb = bb.asFloatBuffer();

        int stride = SimulationConfig.PARTICLE_STRIDE_FLOATS;
        for (int i = 0; i < count; i++) {
            int base = i * stride;

            float[] p = samplePosition(dist);

            // position (OFFSET_POS .. +3)
            int pBase = base + SimulationConfig.OFFSET_POS;
            fb.put(pBase, p[0]);
            fb.put(pBase + 1, p[1]);
            fb.put(pBase + 2, 0f);
            fb.put(pBase + 3, 1f);

            // reset velocity (OFFSET_VEL .. +3)
            int vBase = base + SimulationConfig.OFFSET_VEL;
            fb.put(vBase, 0f);
            fb.put(vBase + 1, 0f);
            fb.put(vBase + 2, 0f);
            fb.put(vBase + 3, 0f);
        }
        GL15.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
    }

    private static float[] samplePosition(Distribution dist) {
        switch (dist) {
            case UNIFORM:
                return sampleUniformPosition();
            case CENTER_BIASED:
                return sampleCentralPosition();
            case GAUSSIAN:
                return sampleGaussianPosition();
            default:
                return sampleUniformPosition();
        }
    }

    /**
     * Uniform distribution in the square [-1, 1] x [-1, 1]
     */
    private static float[] sampleUniformPosition() {
        float x = (float) (Math.random() * 2.0 - 1.0);
        float y = (float) (Math.random() * 2.0 - 1.0);
        return new float[] { x, y };
    }

    /**
     * Center-biased distribution
     * Uses a radial power law r = U^beta with beta > 0.5
     */
    private static final float CENTER_BIAS_BETA = 1.5f; // > 0.5 biases toward center

    private static float[] sampleCentralPosition() {
        double u = Math.random();
        double theta = Math.random() * Math.PI * 2.0;
        double r = Math.pow(u, CENTER_BIAS_BETA);
        float x = (float) (r * Math.cos(theta));
        float y = (float) (r * Math.sin(theta));
        return new float[] { x, y };
    }

    /**
     * Gaussian (normal) distribution centered at (0,0)
     * Uses Box-Muller transform
     */
    private static float[] sampleGaussianPosition() {
        double u1 = Math.max(1e-6, Math.random());
        double u2 = Math.random();
        double mag = Math.sqrt(-2.0 * Math.log(u1));
        double z0 = mag * Math.cos(2 * Math.PI * u2);
        double z1 = mag * Math.sin(2 * Math.PI * u2);
        float scale = 0.3f;
        return new float[] { (float) (z0 * scale), (float) (z1 * scale) };
    }
}

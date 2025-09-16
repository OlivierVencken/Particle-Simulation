package io.github.simulation.config;

import io.github.simulation.util.PaletteUtil;
/**
 * Configuration constants
 */
public final class SimulationConfig {

    // Simulation parameters
    public static final int PARTICLE_COUNT = 25000;
    public static final float PARTICLE_SIZE_PX = 4.0f;

    // Particle groups
    // Max group count = 16 --> computer shader
    public static final int PARTICLE_GROUPS = 10;
    public static final float[][] GROUP_COLORS = PaletteUtil.generateEvenHue(PARTICLE_GROUPS);

    // Attraction matrix - values between -1 and 1
    // attraction[i][j] = how much group i is attracted to group j
    // Set to identity matrix by default
    //
    // Example:
    // { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f },
    // { 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f },
    // { 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f },
    // { 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f },
    // { 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f },
    // { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f }
    public static final float[][] ATTRACTION_MATRIX = identity(PARTICLE_GROUPS);

    // Physics constants
    public static final float FORCE_FACTOR = 0.2f; // Global force multiplier
    public static final float VELOCITY_DAMPING = 0.95f; // Velocity damping per frame
    public static final float INTERACTION_RANGE = 0.1f; // Interaction range for particles

    // Spatial partitioning grid
    public static final float GRID_CELL_SIZE = INTERACTION_RANGE; // Each cell is the size of interaction range
    public static final int GRID_SIZE = (int) Math.ceil(2.0f / GRID_CELL_SIZE); // Grid spans from -1 to 1 in both
                                                                                // dimensions

    // Calculate particles per cell based on expected density
    // Max particles per cell PARTICLE_COUNT / (GRID_SIZE * GRID_SIZE) *
    // DENSITY_SAFETY_FACTOR
    // Overflow will be ignored
    private static final int TOTAL_CELLS = GRID_SIZE * GRID_SIZE;
    private static final float DENSITY_SAFETY_FACTOR = 10.0f; // Increase for more safety, decrease for performance
    public static final int MAX_PARTICLES_PER_CELL = Math.max(64,
            (int) Math.ceil((PARTICLE_COUNT / (float) TOTAL_CELLS) * DENSITY_SAFETY_FACTOR));

    // Rendering constants
    public static final float[] BACKGROUND_COLOR = { 0f, 0f, 0f, 0f };
    public static final int WORKGROUP_SIZE = 256; 
    public static final int PARTICLE_STRIDE_FLOATS = 16; // vec4 pos + vec4 vel + vec4 col + vec4 type/group

    // Default attraction matrix preset
    private static float[][] identity(int n) {
        float[][] m = new float[n][n];
        for (int i = 0; i < n; i++) {
            m[i][i] = 1.0f;
        }
        return m;
    }
}

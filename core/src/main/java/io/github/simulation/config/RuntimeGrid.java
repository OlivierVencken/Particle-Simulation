package io.github.simulation.config;

/**
 * Runtime grid configuration that updates based on current interaction range
 */
public class RuntimeGrid {

    private static float currentGridCellSize;
    private static int currentGridSize;
    private static int currentMaxParticlesPerCell;
    private static boolean needsGridRebuild = true;

    static {
        updateGridParameters();
    }

    public static void updateGridParameters() {
        float newCellSize = RuntimeConfig.getInteractionRange();
        int newGridSize = (int) Math.ceil(2.0f / newCellSize);

        // Calculate particles per cell based on expected density
        int totalCells = newGridSize * newGridSize;
        float densitySafetyFactor = 4.0f;
        int newMaxParticles = Math.max(64,
                (int) Math.ceil((SimulationConfig.PARTICLE_COUNT / (float) totalCells) * densitySafetyFactor));

        // Check if parameters changed
        if (Math.abs(newCellSize - currentGridCellSize) > 0.001f ||
                newGridSize != currentGridSize ||
                newMaxParticles != currentMaxParticlesPerCell) {

            currentGridCellSize = newCellSize;
            currentGridSize = newGridSize;
            currentMaxParticlesPerCell = newMaxParticles;
            needsGridRebuild = true;
        }
    }

    public static float getGridCellSize() {
        return currentGridCellSize;
    }

    public static int getGridSize() {
        return currentGridSize;
    }

    public static int getMaxParticlesPerCell() {
        return currentMaxParticlesPerCell;
    }

    public static boolean needsGridRebuild() {
        return needsGridRebuild;
    }

    public static void markGridRebuilt() {
        needsGridRebuild = false;
    }

    public static String getGridStatusString() {
        return String.format("Grid: %dx%d (%.3f)",
                currentGridSize, currentGridSize, currentGridCellSize);
    }

    public static String getMaxCellString() {
        return String.format("Max/Cell: %d", currentMaxParticlesPerCell);
    }

}

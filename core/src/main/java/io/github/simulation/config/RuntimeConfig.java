package io.github.simulation.config;

/**
 * Runtime configuration manager for dynamic parameter changes during simulation
 */
public class RuntimeConfig {

    private static float timeScale = 1.0f;
    private static float forceFactor = SimulationConfig.FORCE_FACTOR;
    private static float velocityDamping = SimulationConfig.VELOCITY_DAMPING;
    private static float interactionRange = SimulationConfig.INTERACTION_RANGE;

    private static float[][] attractionMatrix = copyMatrix(SimulationConfig.ATTRACTION_MATRIX);

    private static final float[][][] PRESET_MATRICES = {
            // Total repulsion
            {
                    { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f }
            },
            // Only attract same group
            {
                    { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f }
            },
            // RGB worm 
            {
                    { 1.0f, -0.1f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.2f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.2f, 1.0f, 0.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.2f, 1.0f, 0.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.2f, 1.0f, 0.0f },
                    { 0.0f, 0.0f, 0.0f, 0.0f, 0.2f, 1.0f }
            },
            // Max attraction with self-repulsion
            {
                    { -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f },
                    { 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f },
                    { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f },
                    { 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f },
                    { 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f },
                    { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f }
            }
    };

    public static float getTimeScale() {
        return timeScale;
    }

    public static float getForceFactor() {
        return forceFactor;
    }

    public static float getVelocityDamping() {
        return velocityDamping;
    }

    public static float getInteractionRange() {
        return interactionRange;
    }

    public static float[][] getAttractionMatrix() {
        return attractionMatrix;
    }

    public static void setTimeScale(float value) {
        timeScale = Math.max(0.1f, Math.min(5.0f, value));
    }

    public static void setForceFactor(float value) {
        forceFactor = Math.max(0.01f, Math.min(10.0f, value));
    }

    public static void setVelocityDamping(float value) {
        velocityDamping = Math.max(0.9f, Math.min(0.999f, value));
    }

    public static void setInteractionRange(float value) {
        interactionRange = Math.max(0.005f, Math.min(1.0f, value));
    }

    public static void increaseTimeScale() {
        setTimeScale(timeScale * 1.1f);
    }

    public static void decreaseTimeScale() {
        setTimeScale(timeScale * 0.9f);
    }

    public static void increaseForceFactor() {
        setForceFactor(forceFactor * 1.1f);
    }

    public static void decreaseForceFactor() {
        setForceFactor(forceFactor * 0.9f);
    }

    public static void increaseVelocityDamping() {
        setVelocityDamping(velocityDamping + 0.001f);
    }

    public static void decreaseVelocityDamping() {
        setVelocityDamping(velocityDamping - 0.001f);
    }

    public static void increaseInteractionRange() {
        setInteractionRange(interactionRange + 0.01f);
    }

    public static void decreaseInteractionRange() {
        setInteractionRange(interactionRange - 0.01f);
    }

    public static void randomizeAttractionMatrix() {
        for (int i = 0; i < SimulationConfig.PARTICLE_GROUPS; i++) {
            for (int j = 0; j < SimulationConfig.PARTICLE_GROUPS; j++) {
                attractionMatrix[i][j] = (float) (Math.random() * 2.0 - 1.0); // -1 to 1
            }
        }
    }

    public static void loadPreset(int presetIndex) {
        if (presetIndex >= 0 && presetIndex < PRESET_MATRICES.length) {
            attractionMatrix = copyMatrix(PRESET_MATRICES[presetIndex]);
        }
    }

    public static void resetToDefaults() {
        timeScale = 1.0f;
        forceFactor = SimulationConfig.FORCE_FACTOR;
        velocityDamping = SimulationConfig.VELOCITY_DAMPING;
        interactionRange = SimulationConfig.INTERACTION_RANGE;
        attractionMatrix = copyMatrix(SimulationConfig.ATTRACTION_MATRIX);
    }

    private static float[][] copyMatrix(float[][] source) {
        float[][] copy = new float[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    public static String getStatusString() {
        return String.format(
                "Time: %.2f | Force: %.3f | Damping: %.3f | Range: %.2f",
                timeScale, forceFactor, velocityDamping, interactionRange);
    }
}
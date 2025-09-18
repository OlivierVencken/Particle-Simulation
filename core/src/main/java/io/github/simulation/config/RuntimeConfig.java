package io.github.simulation.config;

import io.github.simulation.particles.ParticleSystem;
import io.github.simulation.util.PaletteUtil;

/**
 * Runtime configuration manager for dynamic parameter changes during simulation
 */
public class RuntimeConfig {

    private static int particleCount = SimulationConfig.PARTICLE_COUNT;
    private static float particleSizePx = SimulationConfig.PARTICLE_SIZE_PX;

    private static int groupCount = SimulationConfig.PARTICLE_GROUPS;
    private static float[][] groupColors = SimulationConfig.GROUP_COLORS;
    private static boolean groupsChanged = false;

    private static float timeScale = 1.0f;
    private static float forceFactor = SimulationConfig.FORCE_FACTOR;
    private static float velocityDamping = SimulationConfig.VELOCITY_DAMPING;
    private static float interactionRange = SimulationConfig.INTERACTION_RANGE;

    private static float[][] attractionMatrix = copyMatrix(SimulationConfig.ATTRACTION_MATRIX);
    private static Distribution distribution = SimulationConfig.DISTRIBUTION;

    public enum Distribution {
        UNIFORM,
        CENTER_BIASED,
        GAUSSIAN
    }

    public static int getParticleCount() {
        return particleCount;
    }

    public static void setParticleCount(int value) {
        particleCount = Math.max(0, value);
    }

    public static float getParticleSizePx() {
        return particleSizePx;
    }

    public static void setParticleSizePx(float v) {
        particleSizePx = Math.clamp(v, 1.0f, 32.0f);
    }

    public static void increaseParticleSize() {
        setParticleSizePx(particleSizePx + 0.5f);
    }

    public static void decreaseParticleSize() {
        setParticleSizePx(particleSizePx - 0.5f);
    }

    public static int getGroupCount() {
        return groupCount;
    }

    public static float[][] getGroupColors() {
        return groupColors;
    }

    public static void increaseGroupCount() {
        setGroupCount(groupCount + 1);
    }

    public static void decreaseGroupCount() {
        setGroupCount(groupCount - 1);
    }

    public static void setGroupCount(int newCount) {
        newCount = Math.clamp(newCount, 1, SimulationConfig.MAX_GROUPS);
        if (newCount == groupCount) {
            return;
        }
        groupCount = newCount;
        // Reset attraction matrix to identity (default)
        attractionMatrix = buildIdentity(groupCount);
        // Regenerate palette
        groupColors = PaletteUtil.generateEvenHue(groupCount);
        groupsChanged = true;
    }

    public static boolean consumeGroupsChanged() {
        if (groupsChanged) {
            groupsChanged = false;
            return true;
        }
        return false;
    }

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
        for (int i = 0; i < RuntimeConfig.getGroupCount(); i++) {
            for (int j = 0; j < RuntimeConfig.getGroupCount(); j++) {
                attractionMatrix[i][j] = (float) (Math.random() * 2.0 - 1.0); // -1 to 1
            }
        }
    }

    public static void loadPreset(int idx) {
        int g = SimulationConfig.PARTICLE_GROUPS;
        switch (idx) {
            case 0:
                attractionMatrix = buildZero(g);
                break;
            case 1:
                attractionMatrix = buildIdentity(g);
                break;
            case 2:
                attractionMatrix = buildRing(g);
                break;
            case 3:
                attractionMatrix = buildSelfRepelOthersAttract(g);
                break;
            default:
                attractionMatrix = buildIdentity(g);
                break;
        }
    }

    private static float[][] buildZero(int g) {
        return new float[g][g];
    }

    protected static float[][] buildIdentity(int g) {
        float[][] m = new float[g][g];
        for (int i = 0; i < g; i++) {
            m[i][i] = 1f;
        }
        return m;
    }

    private static float[][] buildRing(int g) {
        float[][] m = new float[g][g];
        for (int i = 0; i < g; i++) {
            m[i][i] = 0.6f;
            m[i][(i + 1) % g] = 0.4f;
        }
        return m;
    }

    private static float[][] buildSelfRepelOthersAttract(int g) {
        float[][] m = new float[g][g];
        for (int i = 0; i < g; i++) {
            for (int j = 0; j < g; j++) {
                m[i][j] = (i == j) ? -1.0f : 0.8f;
            }
        }
        return m;
    }

    public static Distribution getDistribution() {
        return distribution;
    }

    public static void setDistribution(Distribution d) {
        if (d != null) {
            distribution = d;
        }
    }

    public static void resetToDefaults(ParticleSystem particleSystem) {
        timeScale = 1.0f;
        forceFactor = SimulationConfig.FORCE_FACTOR;
        velocityDamping = SimulationConfig.VELOCITY_DAMPING;
        interactionRange = SimulationConfig.INTERACTION_RANGE;
        attractionMatrix = copyMatrix(SimulationConfig.ATTRACTION_MATRIX);
        particleSystem.repositionAllParticles(distribution);
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
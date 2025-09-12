package io.github.simulation.util;

import io.github.simulation.config.SimulationConfig;

/**
 * Utility class for the attraction matrix and particle groups
 */
public class AttractionMatrixUtil {
    
    public static float getAttraction(int fromGroup, int toGroup) {
        if (fromGroup < 0 || fromGroup >= SimulationConfig.PARTICLE_GROUPS ||
            toGroup < 0 || toGroup >= SimulationConfig.PARTICLE_GROUPS) {
            return 0.0f;
        }
        return SimulationConfig.ATTRACTION_MATRIX[fromGroup][toGroup];
    }
    
    public static float[] getGroupColor(int group) {
        if (group < 0 || group >= SimulationConfig.PARTICLE_GROUPS) {
            return new float[]{1.0f, 1.0f, 1.0f, 1.0f}; // White as default
        }
        return SimulationConfig.GROUP_COLORS[group];
    }
}


package io.github.simulation.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import io.github.simulation.config.RuntimeConfig;
import io.github.simulation.particles.ParticleSystem;

/**
 * Handles keyboard input 
 */
public class SimulationInputProcessor extends InputAdapter {

    private final ParticleSystem particleSystem;
    public SimulationInputProcessor(ParticleSystem ps) {
        this.particleSystem = ps;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            // Simulation speed control
            case Input.Keys.RIGHT:
                RuntimeConfig.increaseTimeScale();
                return true;
            case Input.Keys.LEFT:
                RuntimeConfig.decreaseTimeScale();
                return true;

            // Force factor control
            case Input.Keys.UP:
                RuntimeConfig.increaseForceFactor();
                return true;
            case Input.Keys.DOWN:
                RuntimeConfig.decreaseForceFactor();
                return true;

            // Velocity damping control
            case Input.Keys.PERIOD:
                RuntimeConfig.increaseVelocityDamping();
                return true;
            case Input.Keys.COMMA:
                RuntimeConfig.decreaseVelocityDamping();
                return true;

            // Interaction range control
            case Input.Keys.L:
                RuntimeConfig.increaseInteractionRange();
                return true;
            case Input.Keys.K:
                RuntimeConfig.decreaseInteractionRange();
                return true;
            
            // Particle count control
            case Input.Keys.EQUALS: 
            case Input.Keys.PLUS:
                particleSystem.addRandomParticles(500);
                return true;
            case Input.Keys.MINUS:  
                particleSystem.removeRandomParticles(500);
                return true;

            // Particle size control
            case Input.Keys.PAGE_UP:
                RuntimeConfig.increaseParticleSize();
                return true;
            case Input.Keys.PAGE_DOWN:
                RuntimeConfig.decreaseParticleSize();
                return true;

            // Group count control
            case Input.Keys.LEFT_BRACKET: // decrease groups
                RuntimeConfig.decreaseGroupCount();
                return true;
            case Input.Keys.RIGHT_BRACKET: // increase groups
                RuntimeConfig.increaseGroupCount();
                return true;

            // Attraction matrix control
            case Input.Keys.SPACE:
                RuntimeConfig.randomizeAttractionMatrix();
                return true;

            // Preset controls
            case Input.Keys.NUM_1:
                RuntimeConfig.loadPreset(0);
                return true;
            case Input.Keys.NUM_2:
                RuntimeConfig.loadPreset(1);
                return true;
            case Input.Keys.NUM_3:
                RuntimeConfig.loadPreset(2);
                return true;
            case Input.Keys.NUM_4:
                RuntimeConfig.loadPreset(3);
                return true;

            // Reset 
            case Input.Keys.R:
                RuntimeConfig.resetToDefaults();
                return true;

            // Exit
            case Input.Keys.ESCAPE:
                Gdx.app.exit();
                return true;
        }
        return false;
    }
}
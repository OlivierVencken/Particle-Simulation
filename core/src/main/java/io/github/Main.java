package io.github;

import com.badlogic.gdx.ApplicationAdapter;
import io.github.simulation.Simulation;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all
 * platforms.
 */
public class Main extends ApplicationAdapter {
    private Simulation simulation;

    @Override
    public void create() {
        simulation = new Simulation();
        simulation.create();
    }

    @Override
    public void render() {
        simulation.render();
    }

    @Override
    public void dispose() {
        simulation.dispose();
    }
}

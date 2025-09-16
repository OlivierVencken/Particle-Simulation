package io.github.simulation;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.simulation.config.RuntimeConfig;
import io.github.simulation.config.SimulationConfig;
import io.github.simulation.config.RuntimeGrid;
import io.github.simulation.gl.GLCapabilitiesChecker;
import io.github.simulation.gl.GLStateManager;
import io.github.simulation.input.SimulationInputProcessor;
import io.github.simulation.particles.ParticleSystem;
import io.github.simulation.render.ComputeRenderer;
import io.github.simulation.render.GLRenderer;
import io.github.simulation.shader.ShaderManager;

/**
 * Main particle simulation class
 */
public class Simulation extends ApplicationAdapter {
    // Input handling
    private SimulationInputProcessor simInputProcessor;

    // Core systems
    private GLCapabilitiesChecker capabilitiesChecker;
    private ShaderManager shaderManager;
    private ParticleSystem particleSystem;
    private ComputeRenderer computeRenderer;
    private GLRenderer glRenderer;

    // State
    private float time = 0f;
    private boolean canRun = false;

    // UI components
    private SpriteBatch uiBatch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;

    @Override
    public void create() {
        capabilitiesChecker = new GLCapabilitiesChecker();
        canRun = capabilitiesChecker.checkCapabilities(true);

        if (!canRun) {
            Gdx.app.error("Simulation", "Cannot run - insufficient OpenGL capabilities.");
            return;
        }

        shaderManager = new ShaderManager();
        if (!shaderManager.initializeShaders()) {
            canRun = false;
            return;
        }

        particleSystem = new ParticleSystem();
        if (!particleSystem.initialize()) {
            canRun = false;
            return;
        }

        computeRenderer = new ComputeRenderer(shaderManager.getComputeProgram());
        glRenderer = new GLRenderer(shaderManager.getRenderProgram(), shaderManager.getBlitProgram());

        if (!glRenderer.initialize()) {
            canRun = false;
            return;
        }

        // Initialize UI components
        uiBatch = new SpriteBatch();
        font = new BitmapFont();
        shapeRenderer = new ShapeRenderer();

        setupInputHandling();
    }

    private void setupInputHandling() {
        simInputProcessor = new SimulationInputProcessor();
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(simInputProcessor);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render() {
        if (!canRun) {
            Gdx.app.error("Simulation", "Cannot run - exiting.");
            Gdx.app.exit();
            return;
        }

        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;

        particleSystem.bindSSBO();

        particleSystem.checkAndRebuildGrid();

        particleSystem.clearGrid();

        computeRenderer.executeComputeShader(deltaTime, time);

        glRenderer.render();

        particleSystem.unbindSSBO();

        renderStatusOverlay();
    }

    private void renderStatusOverlay() {
        GLStateManager.ensureSpriteBatchCompatibility();

        float[][] matrix = RuntimeConfig.getAttractionMatrix();
        int groupCount = (matrix != null) ? matrix.length : 0;

        // Layout constants
        final float paddingX = 10f;
        final float lineHeight = 20f;
        final float minTextWidth = 300f;
        final int cellSize = 18;

        int lineCount = 9 + 2; // 9 text + 2 spacing

        float statsStartY = Gdx.graphics.getHeight() - 5f;

        float matrixStartX = 200f;
        float matrixStartY = Gdx.graphics.getHeight() - 30f;
        float matrixWidth = groupCount * cellSize;
        float matrixHeight = groupCount * cellSize;

        float textHeight = lineCount * lineHeight - 5f;
        float statsBackgroundWidth = Math.max(minTextWidth, matrixStartX + matrixWidth) + 15f;
        float statsBackgroundHeight = Math.max(textHeight, matrixHeight);

        // Semi-transparent background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(0, statsStartY - statsBackgroundHeight, statsBackgroundWidth, statsBackgroundHeight + 10f);
        shapeRenderer.end();

        if (groupCount > 0) {
            renderAttractionMatrix((int) matrixStartX, (int) matrixStartY, cellSize);
        }

        // Text overlay
        uiBatch.begin();
        float y = statsStartY;

        font.draw(uiBatch, String.format("FPS: %d", Gdx.graphics.getFramesPerSecond()), paddingX, y);
        y -= lineHeight;
        y -= lineHeight;

        font.draw(uiBatch, String.format("Particle Count: %d", SimulationConfig.PARTICLE_COUNT), paddingX, y);
        y -= lineHeight;
        font.draw(uiBatch, String.format("Groups: %d", groupCount), paddingX, y);
        y -= lineHeight;
        y -= lineHeight;

        font.draw(uiBatch, String.format("Time Scale: %.2f", RuntimeConfig.getTimeScale()), paddingX, y);
        y -= lineHeight;
        font.draw(uiBatch, String.format("Force Factor: %.3f", RuntimeConfig.getForceFactor()), paddingX, y);
        y -= lineHeight;
        font.draw(uiBatch, String.format("Velocity Damping: %.3f", RuntimeConfig.getVelocityDamping()), paddingX, y);
        y -= lineHeight;
        font.draw(uiBatch, String.format("Interaction Range: %.2f", RuntimeConfig.getInteractionRange()), paddingX, y);
        y -= lineHeight;
        font.draw(uiBatch, RuntimeGrid.getGridStatusString(), paddingX, y);
        y -= lineHeight;
        font.draw(uiBatch, RuntimeGrid.getMaxCellString(), paddingX, y);
        y -= lineHeight;

        uiBatch.end();

        GLStateManager.ensureComputeRenderState();
    }

    private void renderAttractionMatrix(int startX, int startY, int cellSize) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float[][] matrix = RuntimeConfig.getAttractionMatrix();
        if (matrix == null) {
            shapeRenderer.end();
            return;
        }

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                Color gridColor = getGridColor(matrix[i][j]);
                shapeRenderer.setColor(gridColor);
                shapeRenderer.rect(startX + j * cellSize, startY - i * cellSize, cellSize - 1, cellSize - 1);
            }
        }
        shapeRenderer.end();
    }

    private Color getGridColor(float value) {
        if (value > 0) {
            // Green for attraction
            return new Color(0, 0.5f, 0, Math.min(Math.abs(value), 1.0f));
        } else if (value < 0) {
            // Red for repulsion
            return new Color(0.5f, 0, 0, Math.min(Math.abs(value), 1.0f));
        } else {
            // Gray for neutral
            return new Color(0.3f, 0.3f, 0.3f, 0.5f);
        }
    }

    @Override
    public void dispose() {
        if (shaderManager != null) {
            shaderManager.dispose();
        }
        if (particleSystem != null) {
            particleSystem.dispose();
        }
        if (glRenderer != null) {
            glRenderer.dispose();
        }
        if (uiBatch != null) {
            uiBatch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}

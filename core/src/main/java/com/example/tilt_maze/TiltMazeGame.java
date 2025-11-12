package com.example.tilt_maze;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.utils.Array;

/**
 * TiltMazeGame (principal) - versión con reinicio corregido.
 * Coordina Player, PlatformManager e InputManager.
 */
public class TiltMazeGame extends ApplicationAdapter {

    // Estados del juego
    private enum GameState { START, PLAY, WIN, GAME_OVER }
    private GameState state = GameState.START;

    // Gráficos / cámara
    private OrthographicCamera camera;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;

    // Componentes del juego (modularizados)
    private Player player;
    private PlatformManager platformManager;
    private InputManager inputManager;

    // Contador monedas
    private int coinsCollected = 0;

    @Override
    public void create() {
        // Cámara vertical
        camera = new OrthographicCamera(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        camera.position.set(GameConfig.WORLD_WIDTH / 2f, GameConfig.WORLD_HEIGHT / 2f, 0);
        camera.update();

        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        // Inicializamos manager de plataformas y generamos el nivel inicial
        platformManager = new PlatformManager();
        platformManager.init();

        // Player sobre la base (usamos la primera plataforma como referencia)
        Array<Platform> initialPlatforms = platformManager.getPlatforms();
        Platform base = initialPlatforms.size > 0 ? initialPlatforms.get(0) : null;
        float startX = (base != null) ? base.getX() + base.getWidth() / 2f - 16f : GameConfig.WORLD_WIDTH / 2f - 16f;
        float startY = (base != null) ? base.getY() + base.getHeight() : 100f;

        player = new Player(startX, startY, 32f, 32f);
        inputManager = new InputManager();

        coinsCollected = 0;

        // Log informativo sobre giroscopio
        boolean hasGyro = Gdx.input.isPeripheralAvailable(Input.Peripheral.Gyroscope);
        Gdx.app.log("TiltJump", "Gyroscope available: " + hasGyro);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        update(dt);

        // Dibujado
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // fondo
        shapes.setColor(new Color(0.1f, 0.1f, 0.18f, 1f));
        shapes.rect(
            camera.position.x - GameConfig.WORLD_WIDTH / 2f,
            camera.position.y - GameConfig.WORLD_HEIGHT / 2f,
            GameConfig.WORLD_WIDTH,
            GameConfig.WORLD_HEIGHT
        );

        // plataformas
        shapes.setColor(new Color(0.25f, 0.25f, 0.35f, 1f));
        for (Platform p : platformManager.getPlatforms()) {
            shapes.rect(p.getX(), p.getY(), p.getWidth(), p.getHeight());
        }

        // monedas
        shapes.setColor(Color.GOLD);
        for (Coin c : platformManager.getCoins()) {
            if (!c.isCollected()) shapes.circle(c.circle.x, c.circle.y, c.circle.radius, 20);
        }

        // jugador
        shapes.setColor(Color.SKY);
        shapes.rect(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        shapes.end();

        // HUD
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch,
            "Monedas: " + coinsCollected + " / " + GameConfig.TARGET_COINS,
            camera.position.x - GameConfig.WORLD_WIDTH / 2f + 20,
            camera.position.y + GameConfig.WORLD_HEIGHT / 2f - 20
        );
        font.draw(batch,
            "Control: Giroscopio",
            camera.position.x - GameConfig.WORLD_WIDTH / 2f + 20,
            camera.position.y + GameConfig.WORLD_HEIGHT / 2f - 50
        );

        if (state == GameState.START) {
            font.draw(batch, "DOODLE JUMP SENSOR", camera.position.x - 90, camera.position.y + 40);
            font.draw(batch, "Inclina/gira el celular para moverte", camera.position.x - 150, camera.position.y + 10);
            font.draw(batch, "Recoge " + GameConfig.TARGET_COINS + " monedas para ganar", camera.position.x - 150, camera.position.y - 20);
            font.draw(batch, "Toca la pantalla para empezar", camera.position.x - 150, camera.position.y - 50);
        } else if (state == GameState.WIN) {
            font.draw(batch, "¡GANASTE! :D", camera.position.x - 70, camera.position.y + 20);
            font.draw(batch, "Toca para reiniciar", camera.position.x - 90, camera.position.y - 10);
        } else if (state == GameState.GAME_OVER) {
            font.draw(batch, "GAME OVER :(", camera.position.x - 80, camera.position.y + 20);
            font.draw(batch, "Toca para reiniciar", camera.position.x - 90, camera.position.y - 10);
        }

        batch.end();
    }

    private void update(float dt) {
        // Salir rápido en PC/emulador
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();

        switch (state) {
            case START:
                if (Gdx.input.justTouched()) {
                    // Iniciamos la partida desde START → PLAY
                    state = GameState.PLAY;
                    // Reseteamos cualquier tilt acumulado
                    inputManager.reset();
                }
                return;

            case WIN:
            case GAME_OVER:
                if (Gdx.input.justTouched()) {
                    // ---------- REINICIO CORREGIDO ----------
                    // 1) Reiniciamos plataformas y monedas
                    platformManager.init();

                    // 2) Reposicionamos al jugador sobre la base (primera plataforma)
                    Platform base = platformManager.getPlatforms().get(0);
                    player.setPosition(base.getX() + base.getWidth() / 2f - 16f,
                        base.getY() + base.getHeight());

                    // 3) Reseteamos la velocidad vertical para evitar caídas instantáneas
                    player.setVy(GameConfig.JUMP_VELOCITY);

                    // 4) Reiniciamos el contador de monedas
                    coinsCollected = 0;

                    // 5) Reseteamos el input (tilt virtual)
                    inputManager.reset();

                    // 6) Reiniciamos la cámara al centro inicial (muy importante)
                    camera.position.set(GameConfig.WORLD_WIDTH / 2f, GameConfig.WORLD_HEIGHT / 2f, 0f);
                    camera.update();

                    // 7) Volvemos a estado START para que el jugador vea las instrucciones
                    state = GameState.START;
                    // ---------- FIN REINICIO ----------
                }
                return;

            case PLAY:
                // Flujo normal de juego
                float inputX = inputManager.readHorizontal(dt);
                player.update(dt, inputX, platformManager.getPlatforms());

                // Cámara sigue hacia arriba (no baja)
                if (player.getY() > camera.position.y) camera.position.y = player.getY();
                camera.update();

                // Generar plataformas arriba y limpiar abajo
                float targetY = camera.position.y + GameConfig.WORLD_HEIGHT * GameConfig.PLATFORM_SPAWN_AHEAD_SCREENS;
                platformManager.generateUpTo(targetY);

                float removeBelowY = camera.position.y - GameConfig.WORLD_HEIGHT * GameConfig.CLEANUP_BELOW_SCREENS;
                platformManager.cleanupBelow(removeBelowY);

                // Monedas y estados
                checkCoinsCollected();
                checkWinOrFall();
                break;
        }
    }

    private void checkCoinsCollected() {
        float px = player.centerX();
        float py = player.centerY();
        float pr = player.radiusApprox();

        for (Coin c : platformManager.getCoins()) {
            if (c.isCollected()) continue;
            float dx = px - c.circle.x;
            float dy = py - c.circle.y;
            float dist2 = dx * dx + dy * dy;
            float sumR = pr + c.circle.radius;
            if (dist2 <= sumR * sumR) {
                c.collect();
                coinsCollected++;
            }
        }
    }

    private void checkWinOrFall() {
        if (coinsCollected >= GameConfig.TARGET_COINS) {
            state = GameState.WIN;
            return;
        }

        float bottomLimit = camera.position.y - GameConfig.WORLD_HEIGHT / 2f - 150f;
        if (player.getY() + player.getHeight() < bottomLimit) {
            state = GameState.GAME_OVER;
        }
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}

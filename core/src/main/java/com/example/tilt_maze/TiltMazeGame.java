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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.utils.Array;

public class TiltMazeGame extends ApplicationAdapter {

    // ------------------------------
    // 1) ESTADOS DEL JUEGO
    // ------------------------------
    private enum GameState {
        START,
        PLAY,
        WIN,
        GAME_OVER
    }

    private GameState state = GameState.START;

    // ------------------------------
    // 2) MUNDO / CÁMARA (VERTICAL)
    // ------------------------------
    private static final float WORLD_WIDTH  = 480f;
    private static final float WORLD_HEIGHT = 800f;

    private OrthographicCamera camera;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;

    // ------------------------------
    // 3) CONSTANTES MODIFICABLES (PARA LOS ALUMNOS)
    // ------------------------------
    // Cantidad de monedas necesarias para ganar
    private static final int TARGET_COINS = 15;

    // Color del personaje (pueden cambiarlo)
    private Color playerColor = Color.SKY;

    // Física vertical
    private static final float GRAVITY       = -900f;
    private static final float JUMP_VELOCITY = 550f;
    private static final float MAX_H_SPEED   = 220f;

    // Giroscopio: qué tan fuerte se traduce la rotación a movimiento
    private static final float GYRO_SENSITIVITY = 8.5f;

    // Parámetros de plataformas (endless)
    private static final float PLATFORM_WIDTH      = 120f;
    private static final float PLATFORM_HEIGHT     = 18f;
    private static final float PLATFORM_STEP_Y     = 120f;
    // Probabilidad de que una plataforma tenga moneda (0.4 = 40%)
    private static final float COIN_SPAWN_CHANCE   = 0.40f;

    // ------------------------------
    // 4) JUGADOR
    // ------------------------------
    private Rectangle player;
    private float playerVy;

    // ------------------------------
    // 5) PLATAFORMAS Y MONEDAS
    // ------------------------------
    private Array<Rectangle> platforms;
    private Array<Circle> coins;

    private int coinsCollected = 0;

    // Altura Y donde se colocará la siguiente plataforma nueva
    private float nextPlatformY = 0f;

    // ------------------------------
    // 6) CONTROL CON GIROSCOPIO
    // ------------------------------
    // "Ángulo virtual" que usamos como inclinación izquierda/derecha.
    private float virtualTiltX = 0f;

    @Override
    public void create() {
        camera = new OrthographicCamera(WORLD_WIDTH, WORLD_HEIGHT);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        platforms = new Array<>();
        coins = new Array<>();

        // Solo informativo: ver si el dispositivo tiene giroscopio
        boolean hasGyro = Gdx.input.isPeripheralAvailable(Input.Peripheral.Gyroscope);
        Gdx.app.log("TiltJump", "Gyroscope available: " + hasGyro);

        initLevel();
    }

    /**
     * Crea las plataformas iniciales, algunas monedas y resetea al jugador.
     */
    private void initLevel() {
        platforms.clear();
        coins.clear();
        coinsCollected = 0;
        virtualTiltX = 0f;

        // Plataforma base
        Rectangle ground = new Rectangle(0, 80, WORLD_WIDTH, 20);
        platforms.add(ground);

        // La siguiente plataforma se generará más arriba
        nextPlatformY = 140f;

        // Generamos un "colchón" de plataformas por encima de la cámara inicial
        float targetFirstPlatformsY = WORLD_HEIGHT * 2f;
        while (nextPlatformY < targetFirstPlatformsY) {
            createPlatformWithOptionalCoin(nextPlatformY);
            nextPlatformY += PLATFORM_STEP_Y;
        }

        // Jugador sobre la base
        player = new Rectangle(
            ground.x + ground.width / 2f - 16f,
            ground.y + ground.height,
            32f,
            32f
        );

        playerVy = JUMP_VELOCITY;
        state = GameState.START;

        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        update(dt);

        Gdx.gl.glClearColor(0.06f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        // ------------------------------
        // DIBUJO DE ESCENA
        // ------------------------------
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Fondo
        shapes.setColor(new Color(0.1f, 0.1f, 0.18f, 1f));
        shapes.rect(
            camera.position.x - WORLD_WIDTH / 2f,
            camera.position.y - WORLD_HEIGHT / 2f,
            WORLD_WIDTH,
            WORLD_HEIGHT
        );

        // Plataformas
        shapes.setColor(new Color(0.25f, 0.25f, 0.35f, 1f));
        for (Rectangle p : platforms) {
            shapes.rect(p.x, p.y, p.width, p.height);
        }

        // Monedas
        shapes.setColor(Color.GOLD);
        for (Circle c : coins) {
            if (c.radius > 0f) {
                shapes.circle(c.x, c.y, c.radius, 20);
            }
        }

        // Jugador
        shapes.setColor(playerColor);
        shapes.rect(player.x, player.y, player.width, player.height);

        shapes.end();

        // ------------------------------
        // DIBUJO DE HUD
        // ------------------------------
        batch.begin();
        font.setColor(Color.WHITE);

        font.draw(batch,
            "Monedas: " + coinsCollected + " / " + TARGET_COINS,
            camera.position.x - WORLD_WIDTH / 2f + 20,
            camera.position.y + WORLD_HEIGHT / 2f - 20
        );

        font.draw(batch,
            "Control: Giroscopio",
            camera.position.x - WORLD_WIDTH / 2f + 20,
            camera.position.y + WORLD_HEIGHT / 2f - 50
        );

        switch (state) {
            case START:
                font.draw(batch,
                    "DOODLE JUMP SENSOR",
                    camera.position.x - 90,
                    camera.position.y + 40
                );
                font.draw(batch,
                    "Inclina/gira el celular para moverte",
                    camera.position.x - 150,
                    camera.position.y + 10
                );
                font.draw(batch,
                    "Recoge " + TARGET_COINS + " monedas para ganar",
                    camera.position.x - 150,
                    camera.position.y - 20
                );
                font.draw(batch,
                    "Toca la pantalla para empezar",
                    camera.position.x - 150,
                    camera.position.y - 50
                );
                break;
            case WIN:
                font.draw(batch,
                    "¡GANASTE! :D",
                    camera.position.x - 70,
                    camera.position.y + 20
                );
                font.draw(batch,
                    "Toca para reiniciar",
                    camera.position.x - 90,
                    camera.position.y - 10
                );
                break;
            case GAME_OVER:
                font.draw(batch,
                    "GAME OVER :(",
                    camera.position.x - 80,
                    camera.position.y + 20
                );
                font.draw(batch,
                    "Toca para reiniciar",
                    camera.position.x - 90,
                    camera.position.y - 10
                );
                break;
            case PLAY:
                // Sin mensaje extra
                break;
        }

        batch.end();
    }

    private void update(float dt) {
        // ESC para salir en desktop
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        if (state == GameState.START) {
            if (Gdx.input.justTouched()) {
                state = GameState.PLAY;
            }
            return;
        }

        if (state == GameState.WIN || state == GameState.GAME_OVER) {
            if (Gdx.input.justTouched()) {
                initLevel();
            }
            return;
        }

        // Si estamos jugando:
        updatePlayer(dt);
        updateCamera();
        generateMorePlatformsIfNeeded();
        cleanupObjectsBelowCamera();
        checkCoinsCollected();
        checkWinOrFall();
    }

    /**
     * Actualiza movimiento del jugador (horizontal por giroscopio + salto).
     */
    private void updatePlayer(float dt) {
        // 1) Movimiento horizontal a partir del giroscopio
        float inputX = readHorizontalFromGyro(dt); // entre -1 y 1
        float vx = inputX * MAX_H_SPEED;

        player.x += vx * dt;

        // Wrap lateral
        if (player.x + player.width < 0) {
            player.x = WORLD_WIDTH;
        } else if (player.x > WORLD_WIDTH) {
            player.x = -player.width;
        }

        // 2) Física vertical
        playerVy += GRAVITY * dt;

        float oldY = player.y;
        player.y += playerVy * dt;

        // Rebote solo al caer
        if (playerVy <= 0f) {
            for (Rectangle p : platforms) {
                boolean wasAbove = oldY >= p.y + p.height;
                boolean nowBelowTop = player.y <= p.y + p.height;
                boolean withinX =
                    player.x + player.width * 0.5f > p.x &&
                        player.x + player.width * 0.5f < p.x + p.width;

                if (wasAbove && nowBelowTop && withinX) {
                    player.y = p.y + p.height;
                    playerVy = JUMP_VELOCITY;
                    break;
                }
            }
        }
    }

    /**
     * Lee el giroscopio y lo convierte en un "ángulo virtual" entre -1 y 1.
     * Cuanto más gires el teléfono, más se mueve el personaje.
     */
    private float readHorizontalFromGyro(float dt) {
        // Rotación alrededor del eje Z (como girar un volante)
        float gyroZ = Gdx.input.getGyroscopeZ(); // radianes/segundo

        // Integramos para acumular una inclinación virtual
        virtualTiltX += -gyroZ * dt * GYRO_SENSITIVITY;

        // Permitimos más inclinación acumulada
        virtualTiltX = MathUtils.clamp(virtualTiltX, -3f, 3f);

        // Pequeño decaimiento para que vuelva poco a poco al centro
        virtualTiltX *= 0.99f;

        // Valor final que usamos: entre -1 y 1
        return MathUtils.clamp(virtualTiltX, -1f, 1f);
    }

    /**
     * La cámara sigue al jugador hacia arriba.
     */
    private void updateCamera() {
        if (player.y > camera.position.y) {
            camera.position.y = player.y;
        }
        camera.update();
    }

    /**
     * Crea una plataforma en la altura indicada y, con cierta probabilidad,
     * una moneda sobre ella.
     */
    private void createPlatformWithOptionalCoin(float y) {
        float x = MathUtils.random(20f, WORLD_WIDTH - PLATFORM_WIDTH - 20f);
        Rectangle p = new Rectangle(x, y, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        platforms.add(p);

        // ¿Esta plataforma tendrá moneda?
        if (MathUtils.random() < COIN_SPAWN_CHANCE) {
            float cx = p.x + p.width / 2f;
            float cy = p.y + p.height + 26f;
            Circle coin = new Circle(cx, cy, 10f);
            coins.add(coin);
        }
    }

    /**
     * Genera más plataformas (y posibles monedas) por encima de la cámara,
     * para que el nivel sea "infinito" hacia arriba.
     */
    private void generateMorePlatformsIfNeeded() {
        // Queremos tener plataformas hasta 1 pantalla por encima de la cámara
        float targetY = camera.position.y + WORLD_HEIGHT;

        while (nextPlatformY < targetY) {
            createPlatformWithOptionalCoin(nextPlatformY);
            nextPlatformY += PLATFORM_STEP_Y;
        }
    }

    /**
     * Elimina plataformas y monedas que quedaron muy por debajo de la cámara
     * (ya no se verán y no se necesitan).
     */
    private void cleanupObjectsBelowCamera() {
        float removeBelowY = camera.position.y - WORLD_HEIGHT * 2f;

        // Plataformas
        for (int i = platforms.size - 1; i >= 0; i--) {
            Rectangle p = platforms.get(i);
            if (p.y + p.height < removeBelowY) {
                platforms.removeIndex(i);
            }
        }

        // Monedas
        for (int i = coins.size - 1; i >= 0; i--) {
            Circle c = coins.get(i);
            if (c.radius <= 0f || c.y + c.radius < removeBelowY) {
                coins.removeIndex(i);
            }
        }
    }

    /**
     * Verifica colisión con monedas.
     */
    private void checkCoinsCollected() {
        float playerCenterX = player.x + player.width / 2f;
        float playerCenterY = player.y + player.height / 2f;
        float playerRadius = player.width * 0.5f;

        for (Circle c : coins) {
            if (c.radius <= 0f) continue;

            float dx = playerCenterX - c.x;
            float dy = playerCenterY - c.y;
            float dist2 = dx * dx + dy * dy;
            float sumR = playerRadius + c.radius;

            if (dist2 <= sumR * sumR) {
                c.radius = 0f;
                coinsCollected++;
            }
        }
    }

    /**
     * Comprueba si ganó o si se cayó demasiado.
     */
    private void checkWinOrFall() {
        if (coinsCollected >= TARGET_COINS) {
            state = GameState.WIN;
            return;
        }

        float bottomLimit = camera.position.y - WORLD_HEIGHT / 2f - 150f;
        if (player.y + player.height < bottomLimit) {
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

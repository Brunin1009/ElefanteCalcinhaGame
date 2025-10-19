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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class TiltMazeGame extends ApplicationAdapter {
    enum GameState { START, PLAY, GAME_OVER, GOAL }

    private OrthographicCamera cam;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;

    private final float WORLD_W = 800f;
    private final float WORLD_H = 480f;

    // Jugador
    private Circle ball;
    private float vx = 0f, vy = 0f;

    // Control por tilt
    private static final float G = 9.81f;          // m/s^2
    private float zeroX = 0f, zeroY = 0f;          // calibración (offset)
    private float filtX = 0f, filtY = 0f;          // filtrado LPF
    private final float lpfAlpha = 0.18f;          // 0..1 (más alto = menos filtro)
    private final float deadZone = 0.05f;          // zona muerta en "g"
    private float sensitivity = 1500f;             // ganancia movimiento
    private float damping = 0.90f;                 // frenado simple

    // Nivel
    private Array<Rectangle> walls;
    private Rectangle goal;

    private GameState state = GameState.START;
    private float playTime = 0f;

    @Override
    public void create() {
        cam = new OrthographicCamera(WORLD_W, WORLD_H);
        cam.position.set(WORLD_W / 2f, WORLD_H / 2f, 0);
        cam.update();

        shapes = new ShapeRenderer();
        batch  = new SpriteBatch();
        font   = new BitmapFont();

        ball = new Circle(80, WORLD_H / 2f, 14);

        walls = new Array<>();
        // Bordes
        walls.add(new Rectangle(0, 0, WORLD_W, 20));
        walls.add(new Rectangle(0, WORLD_H - 20, WORLD_W, 20));
        walls.add(new Rectangle(0, 0, 20, WORLD_H));
        walls.add(new Rectangle(WORLD_W - 20, 0, 20, WORLD_H));
        // Obstáculos internos
        walls.add(new Rectangle(150, 100, 500, 20));
        walls.add(new Rectangle(150, 360, 500, 20));
        walls.add(new Rectangle(150, 120, 20, 240));
        walls.add(new Rectangle(630, 120, 20, 240));
        // Pasillo hacia meta
        walls.add(new Rectangle(700, 120, 20, 80));
        walls.add(new Rectangle(700, 260, 20, 100));

        // Meta (verde)
        goal = new Rectangle(WORLD_W - 100, WORLD_H / 2f - 40, 60, 80);

        // Log de disponibilidad
        Gdx.app.log("TiltMaze", "Accelerometer available: "
            + Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer));

        // Calibra “cero” con el teléfono como está al iniciar
        calibrateZero();
    }

    /** Lee acelerómetro, mapea según rotación y devuelve tilt normalizado en g. */
    private void readTiltG(float[] out2) {
        // Lecturas crudas (m/s^2), incluyen gravedad
        float ax = Gdx.input.getAccelerometerX();
        float ay = Gdx.input.getAccelerometerY();
        float az = Gdx.input.getAccelerometerZ(); // no lo usamos, pero podría servir

        // Mapeo de ejes según rotación de pantalla
        // Ajustado para que inclinar a la DERECHA mueva a la derecha (tx positivo)
        // y inclinar ARRIBA empuje hacia arriba (ty positivo).
        int rot = Gdx.input.getRotation(); // 0, 90, 180, 270
        float tx, ty;
        switch (rot) {
            case 0:     // Portrait natural
                tx = -ax;  // derecha -> tx+
                ty =  ay;  // arriba   -> ty+
                break;
            case 90:    // Landscape, lado "derecho" arriba
                tx =  ay;  // derecha -> tx+
                ty =  ax;  // arriba   -> ty+
                break;
            case 180:   // Portrait invertido
                tx =  ax;  // derecha -> tx+
                ty = -ay;  // arriba   -> ty+
                break;
            case 270:   // Landscape, lado "izquierdo" arriba
                tx = -ay;  // derecha -> tx+
                ty = -ax;  // arriba   -> ty+
                break;
            default:
                tx = -ax;
                ty =  ay;
        }

        // Compensa offset de calibración
        tx -= zeroX;
        ty -= zeroY;

        // Normaliza a múltiplos de "g"
        tx /= G;
        ty /= G;

        // Zona muerta: ignora inclinaciones muy pequeñas
        if (Math.abs(tx) < deadZone) tx = 0f;
        if (Math.abs(ty) < deadZone) ty = 0f;

        // Filtro paso-bajo (suaviza)
        filtX += lpfAlpha * (tx - filtX);
        filtY += lpfAlpha * (ty - filtY);

        out2[0] = filtX;
        out2[1] = filtY;
    }

    /** Guarda el estado actual como “cero” (útil si el usuario sostiene el teléfono inclinado). */
    private void calibrateZero() {
        float ax = Gdx.input.getAccelerometerX();
        float ay = Gdx.input.getAccelerometerY();
        int rot = Gdx.input.getRotation();
        // usa el mismo mapeo que readTiltG() pero sin normalizar/filtrar
        switch (rot) {
            case 0:  zeroX = -ax; zeroY =  ay; break;
            case 90: zeroX =  ay; zeroY =  ax; break;
            case 180:zeroX =  ax; zeroY = -ay; break;
            case 270:zeroX = -ay; zeroY = -ax; break;
            default: zeroX = -ax; zeroY =  ay; break;
        }
        // reinicia filtro para que no “arrastre” valores viejos
        filtX = 0f;
        filtY = 0f;
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        update(dt);

        Gdx.gl.glClearColor(0.06f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Escena
        shapes.setProjectionMatrix(cam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Paredes
        shapes.setColor(new Color(0.15f, 0.18f, 0.24f, 1f));
        for (Rectangle r : walls) shapes.rect(r.x, r.y, r.width, r.height);

        // Meta
        shapes.setColor(state == GameState.GOAL ? Color.GOLD : Color.GREEN);
        shapes.rect(goal.x, goal.y, goal.width, goal.height);

        // Bola
        shapes.setColor(state == GameState.GAME_OVER ? Color.RED : Color.CYAN);
        shapes.circle(ball.x, ball.y, ball.radius, 24);

        shapes.end();

        // HUD
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        switch (state) {
            case START:
                font.draw(batch, "TILT MAZE", WORLD_W * 0.43f, WORLD_H * 0.62f);
                font.draw(batch, "Inclina el celular para mover la bola", WORLD_W * 0.30f, WORLD_H * 0.52f);
                font.draw(batch, "Toca para comenzar (mantén la postura para calibrar)", WORLD_W * 0.20f, WORLD_H * 0.45f);
                break;
            case PLAY:
                font.draw(batch, String.format("Tiempo: %.1f s", playTime), 26, WORLD_H - 26);
                font.draw(batch, "Toca con dos dedos para recalibrar", WORLD_W - 310, 26);
                break;
            case GAME_OVER:
                font.draw(batch, "GAME OVER", WORLD_W * 0.43f, WORLD_H * 0.58f);
                font.draw(batch, "Toca para reiniciar", WORLD_W * 0.40f, WORLD_H * 0.50f);
                break;
            case GOAL:
                font.draw(batch, String.format("¡Ganaste! Tiempo: %.1f s", playTime), WORLD_W * 0.36f, WORLD_H * 0.58f);
                font.draw(batch, "Toca para reiniciar", WORLD_W * 0.40f, WORLD_H * 0.50f);
                break;
        }
        batch.end();
    }

    private void update(float dt) {
        // Atajos: back para salir en desktop, etc. (no crítico en Android)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();

        if (state == GameState.START) {
            if (Gdx.input.justTouched()) {
                // Al tocar en START, toma esa postura como “cero”
                calibrateZero();
                resetGame();
                state = GameState.PLAY;
            }
            return;
        }

        if (state == GameState.GAME_OVER || state == GameState.GOAL) {
            if (Gdx.input.justTouched()) {
                resetGame();
                state = GameState.START;
            }
            return;
        }

        // PLAY
        playTime += dt;

        // Recalibrar en cualquier momento con “multi-touch simple” (2 toques simultáneos)
        if (Gdx.input.isTouched(0) && Gdx.input.isTouched(1)) {
            calibrateZero();
        }

        // Lee tilt normalizado (en g) con mapeo por rotación, offset y filtro
        float[] tilt = new float[2];
        readTiltG(tilt);
        float tiltX = MathUtils.clamp(tilt[0], -1f, 1f);
        float tiltY = MathUtils.clamp(tilt[1], -1f, 1f);

        // Aceleración -> velocidad (ganancia)
        vx += tiltX * sensitivity * dt;
        vy += tiltY * sensitivity * dt;

        // Frenado
        vx *= damping;
        vy *= damping;

        // Integración y colisiones separadas
        float nextX = ball.x + vx * dt;
        float nextY = ball.y + vy * dt;

        float prevX = ball.x;
        ball.x = MathUtils.clamp(nextX, ball.radius, WORLD_W - ball.radius);
        if (collidesAny(ball)) { ball.x = prevX; vx = 0; }

        float prevY = ball.y;
        ball.y = MathUtils.clamp(nextY, ball.radius, WORLD_H - ball.radius);
        if (collidesAny(ball)) { ball.y = prevY; vy = 0; }

        // Derrota por tocar bordes (opcional)
        if (touchesBorder(ball)) state = GameState.GAME_OVER;

        // Victoria
        if (ball.x + ball.radius > goal.x && ball.x - ball.radius < goal.x + goal.width &&
            ball.y + ball.radius > goal.y && ball.y - ball.radius < goal.y + goal.height) {
            state = GameState.GOAL;
        }
    }

    private boolean collidesAny(Circle c) {
        for (Rectangle r : walls) if (circleIntersectsRect(c, r)) return true;
        return false;
    }

    private boolean circleIntersectsRect(Circle c, Rectangle r) {
        float closestX = MathUtils.clamp(c.x, r.x, r.x + r.width);
        float closestY = MathUtils.clamp(c.y, r.y, r.y + r.height);
        float dx = c.x - closestX;
        float dy = c.y - closestY;
        return (dx * dx + dy * dy) <= (c.radius * c.radius);
    }

    private boolean touchesBorder(Circle c) {
        return c.x - c.radius <= 20 || c.x + c.radius >= WORLD_W - 20 ||
            c.y - c.radius <= 20 || c.y + c.radius >= WORLD_H - 20;
    }

    private void resetGame() {
        ball.set(80, WORLD_H / 2f, 14);
        vx = vy = 0f;
        playTime = 0f;
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}

package com.example.tilt_maze;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Player: encapsula rectángulo, velocidad vertical y lógica de movimiento/salto.
 * - update(...) aplica input horizontal, gravedad y rebote con plataformas.
 * - El juego principal (TiltMazeGame) decide cuando comprobar monedas / cámara / estados.
 */
public class Player {
    private final Rectangle rect;
    private float vy;

    public Player(float x, float y, float width, float height) {
        rect = new Rectangle(x, y, width, height);
        vy = GameConfig.JUMP_VELOCITY; // empezar saltando
    }

    public Rectangle getRect() { return rect; }
    public float getX() { return rect.x; }
    public float getY() { return rect.y; }
    public float getWidth() { return rect.width; }
    public float getHeight() { return rect.height; }

    /** Setear posición y (útil para respawn) */
    public void setPosition(float x, float y) {
        rect.x = x;
        rect.y = y;
    }

    public void setVy(float value) { vy = value; }

    /**
     * Actualiza la física del jugador.
     * @param dt delta
     * @param inputX valor entre -1 y 1 del InputManager
     * @param platforms lista de plataformas para chequear rebotes (sólo cuando cae)
     */
    public void update(float dt, float inputX, Array<Platform> platforms) {
        // Movimiento horizontal
        float vx = inputX * GameConfig.MAX_H_SPEED;
        rect.x += vx * dt;

        // wrap lateral
        if (rect.x + rect.width < 0) rect.x = GameConfig.WORLD_WIDTH;
        else if (rect.x > GameConfig.WORLD_WIDTH) rect.x = -rect.width;

        // física vertical
        vy += GameConfig.GRAVITY * dt;
        float oldY = rect.y;
        rect.y += vy * dt;

        // ver colisión con plataformas sólo si va hacia abajo
        if (vy <= 0f) {
            float pxCenter = rect.x + rect.width * 0.5f;
            for (Platform p : platforms) {
                boolean wasAbove = oldY >= p.getY() + p.getHeight();
                boolean nowBelowTop = rect.y <= p.getY() + p.getHeight();
                boolean withinX = pxCenter > p.getX() && pxCenter < p.getX() + p.getWidth();

                if (wasAbove && nowBelowTop && withinX) {
                    // posamos sobre la plataforma y saltamos
                    rect.y = p.getY() + p.getHeight();
                    vy = GameConfig.JUMP_VELOCITY;
                    break;
                }
            }
        }
    }

    /** Centro X del jugador (útil para chequear monedas) */
    public float centerX() { return rect.x + rect.width / 2f; }
    public float centerY() { return rect.y + rect.height / 2f; }
    public float radiusApprox() { return rect.width * 0.5f; }
}

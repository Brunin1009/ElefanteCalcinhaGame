package com.example.tilt_maze;

import com.badlogic.gdx.math.Rectangle;

/**
 * Platform: envoltura simple para la rectángulo de la plataforma.
 * Muy útil para añadir propiedades más tarde (tipo, movimiento, rompedora, etc.)
 */
public class Platform {
    public Rectangle rect;

    public Platform(float x, float y, float width, float height) {
        this.rect = new Rectangle(x, y, width, height);
    }

    public float getX() { return rect.x; }
    public float getY() { return rect.y; }
    public float getWidth() { return rect.width; }
    public float getHeight() { return rect.height; }
}

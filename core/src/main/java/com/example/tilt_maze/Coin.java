package com.example.tilt_maze;

import com.badlogic.gdx.math.Circle;

/**
 * Coin: contenedor simple para una moneda.
 * Si radius == 0 => moneda "recogida".
 */
public class Coin {
    public Circle circle;

    public Coin(float x, float y, float radius) {
        this.circle = new Circle(x, y, radius);
    }

    public boolean isCollected() {
        return circle.radius <= 0f;
    }

    public void collect() {
        circle.radius = 0f;
    }
}

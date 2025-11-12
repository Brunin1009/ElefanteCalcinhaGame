package com.example.tilt_maze;

import com.badlogic.gdx.Gdx;

/**
 * InputManager: encapsula lectura de giroscopio y la "inclinación virtual".
 * - Lleva la integración del gyro y el decay.
 * - Devuelve un valor entre -1 y 1 para mover horizontalmente.
 */
public class InputManager {
    private float virtualTiltX = 0f;

    public InputManager() { }

    /**
     * Llamar cada frame con dt. Devuelve -1..1 donde -1 = izquierda, 1 = derecha.
     */
    public float readHorizontal(float dt) {
        float gyroZ = Gdx.input.getGyroscopeZ(); // rad/s
        virtualTiltX += -gyroZ * dt * GameConfig.GYRO_SENSITIVITY;

        // permitimos acumulado mayor para sensibilidad
        if (virtualTiltX < -3f) virtualTiltX = -3f;
        else if (virtualTiltX > 3f) virtualTiltX = 3f;

        // decay para volver al centro
        virtualTiltX *= 0.99f;

        // volvemos valor usable entre -1 y 1
        if (virtualTiltX < -1f) return -1f;
        if (virtualTiltX > 1f) return 1f;
        return virtualTiltX;
    }

    /** Resetea la calibración/tilt (útil al reiniciar) */
    public void reset() { virtualTiltX = 0f; }
}

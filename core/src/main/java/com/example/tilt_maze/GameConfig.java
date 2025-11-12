package com.example.tilt_maze;

/**
 * GameConfig: todas las constantes "fáciles de cambiar" para los alumnos.
 * Cambiar valores aquí afecta el juego (velocidad, cantidad de monedas, apariencia).
 */
public class GameConfig {
    // Mundo
    public static final float WORLD_WIDTH  = 480f;
    public static final float WORLD_HEIGHT = 800f;

    // Objetivo
    public static final int TARGET_COINS = 15;

    // Jugador / física
    public static final float GRAVITY       = -900f;
    public static final float JUMP_VELOCITY = 550f;
    public static final float MAX_H_SPEED   = 220f;

    // Giroscopio
    public static final float GYRO_SENSITIVITY = 8.5f;

    // Plataformas (endless)
    public static final float PLATFORM_WIDTH    = 120f;
    public static final float PLATFORM_HEIGHT   = 18f;
    public static final float PLATFORM_STEP_Y   = 120f;

    // Monedas
    // Probabilidad de que una plataforma tenga moneda (0..1)
    public static final float COIN_SPAWN_CHANCE = 0.40f;

    // Distancias para gestión (tweaks)
    public static final float PLATFORM_SPAWN_AHEAD_SCREENS = 1.0f; // cuantas pantallas por encima generar
    public static final float CLEANUP_BELOW_SCREENS = 2.0f; // cuantas pantallas por debajo eliminar
}

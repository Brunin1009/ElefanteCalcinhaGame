package com.example.tilt_maze;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * PlatformManager: genera plataformas y monedas "infinite scroll".
 * - Mantiene listas de Platform y Coin.
 * - Genera por encima de la cámara según necesidad.
 * - Limpia objetos demasiado abajo.
 */
public class PlatformManager {
    private final Array<Platform> platforms = new Array<>();
    private final Array<Coin> coins = new Array<>();

    // altura Y donde se colocará la próxima plataforma nueva
    private float nextPlatformY = 0f;

    public PlatformManager() { }

    /**
     * Inicializa la primera capa de plataformas (incluye la base).
     */
    public void init() {
        platforms.clear();
        coins.clear();
        nextPlatformY = 140f;

        // plataforma base
        Platform ground = new Platform(0f, 80f, GameConfig.WORLD_WIDTH, 20f);
        platforms.add(ground);

        // Generar un "colchón" de plataformas por encima
        float targetFirstPlatformsY = GameConfig.WORLD_HEIGHT * 2f;
        while (nextPlatformY < targetFirstPlatformsY) {
            createPlatformWithOptionalCoin(nextPlatformY);
            nextPlatformY += GameConfig.PLATFORM_STEP_Y;
        }
    }

    /** Crea una plataforma en altura y posible moneda encima */
    private void createPlatformWithOptionalCoin(float y) {
        float x = MathUtils.random(20f, GameConfig.WORLD_WIDTH - GameConfig.PLATFORM_WIDTH - 20f);
        Platform p = new Platform(x, y, GameConfig.PLATFORM_WIDTH, GameConfig.PLATFORM_HEIGHT);
        platforms.add(p);

        if (MathUtils.random() < GameConfig.COIN_SPAWN_CHANCE) {
            float cx = p.getX() + p.getWidth() / 2f;
            float cy = p.getY() + p.getHeight() + 26f;
            coins.add(new Coin(cx, cy, 10f));
        }
    }

    /**
     * Se asegura de tener plataformas hasta targetY (se llaman desde el juego).
     */
    public void generateUpTo(float targetY) {
        while (nextPlatformY < targetY) {
            createPlatformWithOptionalCoin(nextPlatformY);
            nextPlatformY += GameConfig.PLATFORM_STEP_Y;
        }
    }

    /**
     * Elimina plataformas y monedas muy por debajo de removeBelowY
     */
    public void cleanupBelow(float removeBelowY) {
        for (int i = platforms.size - 1; i >= 0; i--) {
            Platform p = platforms.get(i);
            if (p.getY() + p.getHeight() < removeBelowY) {
                platforms.removeIndex(i);
            }
        }

        for (int i = coins.size - 1; i >= 0; i--) {
            Coin c = coins.get(i);
            if (c.isCollected() || c.circle.y + c.circle.radius < removeBelowY) {
                coins.removeIndex(i);
            }
        }
    }

    // Exposición de listas para dibujar y colisionar:
    public Array<Platform> getPlatforms() { return platforms; }
    public Array<Coin> getCoins() { return coins; }
}

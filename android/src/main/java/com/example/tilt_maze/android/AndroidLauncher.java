package com.example.tilt_maze.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.example.tilt_maze.TiltMazeGame;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        // Usamos solo giroscopio (tal como pediste)
        config.useGyroscope = true;
        config.useAccelerometer = false;

        initialize(new TiltMazeGame(), config);
    }
}

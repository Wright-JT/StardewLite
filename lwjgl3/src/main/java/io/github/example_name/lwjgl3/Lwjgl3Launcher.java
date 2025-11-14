package io.github.example_name.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.example_name.MainMenu;   // ⬅ use MainMenu instead of Core

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static void createApplication() {
        // ⬅ Start the game at the main menu
        new Lwjgl3Application(new MainMenu(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("StardewLite");

        // Vsync and FPS setup
        configuration.useVsync(true);
        configuration.setForegroundFPS(
            Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1
        );

        // Windowed mode size
        configuration.setWindowedMode(1280, 960);

        // Window icons
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png",
            "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}

package io.github.example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class CurrencyManager {
    private static final String PREF_NAME = "stardewlite_prefs";
    private static final String KEY_CURRENCY = "currency";
    private static int currency = 0;

    // call once at game start (e.g., from Core.create())
    public static void load() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            currency = prefs.getInteger(KEY_CURRENCY, 0);
            if (currency < 0) currency = 0;
        } catch (Exception e) {
            // fallback (desktop headless tests, etc.)
            currency = Math.max(0, currency);
        }
    }

    // call when you want to persist the current currency (e.g., on dispose)
    public static void save() {
        try {
            Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
            prefs.putInteger(KEY_CURRENCY, currency);
            prefs.flush();
        } catch (Exception e) {
            // ignore
        }
    }

    public static int getCurrency() {
        return currency;
    }

    public static void addCurrency(int amount) {
        currency += amount;
        if (currency < 0) currency = 0; // never negative
    }

    public static void setCurrency(int amount) {
        currency = Math.max(0, amount);
    }
}

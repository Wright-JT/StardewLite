package io.github.example_name;

public class CurrencyManager {
    private static int currency = 0;

    public static int getCurrency() {
        return currency;
    }

    public static void addCurrency(int amount) {
        currency += amount;
        if (currency < 0) currency = 0; // prevent negatives
    }

    public static void setCurrency(int amount) {
        currency = Math.max(0, amount);
    }
}

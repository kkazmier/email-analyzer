package model;

/**
 * Poziom ryzyka wiadomości wyznaczany na podstawie sumy punktów.
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * Wyznacza poziom ryzyka na podstawie score.
     * Czysta funkcja - ten sam wynik dla tych samych danych.
     */
    public static RiskLevel fromScore(int score) {
        if (score >= 100) return CRITICAL;
        if (score >= 60)  return HIGH;
        if (score >= 30)  return MEDIUM;
        return LOW;
    }
}

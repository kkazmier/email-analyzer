package model;

/**
 * Wykrycie podejrzanego URL-a w treści wiadomości.
 */
public record UrlFinding(
        String url,
        String reason,
        int weight
) implements Finding {

    @Override
    public String description() {
        return "Podejrzany URL: '%s' - powód: %s".formatted(url, reason);
    }
}

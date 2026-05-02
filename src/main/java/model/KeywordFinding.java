package model;

/**
 * Wykrycie podejrzanego słowa kluczowego w treści lub temacie wiadomości.
 */
public record KeywordFinding(
        String keyword,
        String foundIn,
        int weight
) implements Finding {

    @Override
    public String description() {
        return "Podejrzane słowo kluczowe: '%s' znalezione w: %s"
                .formatted(keyword, foundIn);
    }
}

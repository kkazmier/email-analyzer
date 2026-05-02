package model;

/**
 * Wykrycie domeny zakodowanej w Punycode (xn--).
 * Często stosowane w atakach homograficznych.
 */
public record PunycodeFinding(
        String domain,
        int weight
) implements Finding {

    @Override
    public String description() {
        return "Domena zakodowana w Punycode (możliwy atak homograficzny): '%s'"
                .formatted(domain);
    }
}

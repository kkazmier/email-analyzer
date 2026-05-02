package model;

/**
 * Wykrycie anomalii w nagłówkach / metadanych wiadomości.
 */
public record MetadataFinding(
        String headerName,
        String headerValue,
        String reason,
        int weight
) implements Finding {

    @Override
    public String description() {
        return "Anomalia w nagłówku '%s': wartość='%s', powód: %s"
                .formatted(headerName, headerValue, reason);
    }
}

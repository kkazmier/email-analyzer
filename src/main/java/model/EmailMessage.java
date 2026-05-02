package model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Niemutowalny model wiadomości e-mail.
 *
 * Record automatycznie generuje:
 * - konstruktor,
 * - gettery,
 * - equals, hashCode, toString.
 *
 * Niemutowalność zapewniamy przez:
 * - Map.copyOf() i List.copyOf() w kompaktowym konstruktorze,
 * - brak żadnych setterów (record ich nie ma).
 */
public record EmailMessage(
        String from,
        String replyTo,
        String subject,
        String body,
        List<String> urls,
        Map<String, String> headers
) {

    /**
     * Kompaktowy konstruktor - walidacja i defensywne kopiowanie kolekcji.
     */
    public EmailMessage {
        from      = from      != null ? from.trim()      : "";
        replyTo   = replyTo   != null ? replyTo.trim()   : "";
        subject   = subject   != null ? subject.trim()   : "";
        body      = body      != null ? body             : "";
        urls      = List.copyOf(urls    != null ? urls    : List.of());
        headers   = Map.copyOf(headers  != null ? headers : Map.of());
    }

    /**
     * Bezpieczne pobieranie nagłówka jako Optional.
     */
    public Optional<String> header(String name) {
        return Optional.ofNullable(headers.get(name.toLowerCase()));
    }

    /**
     * Wyciąga domenę z adresu e-mail.
     * Np. "user@example.com" -> Optional.of("example.com")
     */
    public Optional<String> senderDomain() {
        int at = from.indexOf('@');
        if (at < 0 || at == from.length() - 1) return Optional.empty();
        return Optional.of(from.substring(at + 1).toLowerCase());
    }

    /**
     * Wyciąga domenę z adresu Reply-To.
     */
    public Optional<String> replyToDomain() {
        int at = replyTo.indexOf('@');
        if (at < 0 || at == replyTo.length() - 1) return Optional.empty();
        return Optional.of(replyTo.substring(at + 1).toLowerCase());
    }
}

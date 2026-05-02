package analyzer;

import model.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Fabryka analizatorów.
 *
 * Każda metoda zwraca Analyzer (Function<EmailMessage, List<Finding>>).
 * Używamy tutaj:
 * - Function<A,B> jako typ parametrów i zwracanych wartości,
 * - Predicate<T> do warunków,
 * - compose() i andThen() do budowania pipeline'ów,
 * - Stream API wewnątrz reguł,
 * - Optional do bezpiecznego dostępu do danych.
 */
public final class AnalyzerFactory {

    // -------------------------------------------------------------------------
    // Słowa kluczowe
    // -------------------------------------------------------------------------

    private static final Set<String> SUSPICIOUS_KEYWORDS = Set.of(
            "urgent", "verify", "password", "bank", "login",
            "click here", "account suspended", "confirm your",
            "kliknij", "natychmiast", "hasło", "zaloguj",
            "zablokowane", "pilne", "weryfikacja", "podaj dane"
    );

    /**
     * Reguła sprawdzająca słowa kluczowe w temacie i treści.
     *
     * Pokazuje użycie:
     * - własnej funkcji wyższego rzędu (przyjmuje Function i Predicate),
     * - Stream API (filter, map, flatMap),
     * - andThen() do kompozycji.
     */
    public static Analyzer keywordAnalyzer() {
        // Funkcja normalizująca tekst - kompozycja przez andThen
        Function<String, String> normalize =
                ((Function<String, String>) String::trim)
                        .andThen(String::toLowerCase);

        // Predykat sprawdzający czy tekst zawiera podejrzane słowo
        Predicate<String> hasSuspiciousWord = text ->
                SUSPICIOUS_KEYWORDS.stream().anyMatch(text::contains);

        // Funkcja budująca Finding dla danego słowa kluczowego i źródła
        Function<String, Function<String, Finding>> findingBuilder =
                source -> keyword ->
                        new KeywordFinding(keyword, source, 15);

        return email -> {
            String normalizedSubject = normalize.apply(email.subject());
            String normalizedBody    = normalize.apply(email.body());

            // Wykrycia w temacie
            Stream<Finding> subjectFindings = hasSuspiciousWord.test(normalizedSubject)
                    ? SUSPICIOUS_KEYWORDS.stream()
                    .filter(normalizedSubject::contains)
                    .map(findingBuilder.apply("subject"))
                    : Stream.empty();

            // Wykrycia w treści
            Stream<Finding> bodyFindings = hasSuspiciousWord.test(normalizedBody)
                    ? SUSPICIOUS_KEYWORDS.stream()
                    .filter(normalizedBody::contains)
                    .map(findingBuilder.apply("body"))
                    : Stream.empty();

            return Stream.concat(subjectFindings, bodyFindings).toList();
        };
    }

    // -------------------------------------------------------------------------
    // URL
    // -------------------------------------------------------------------------

    /**
     * Reguła analizująca URL-e w wiadomości.
     *
     * Sprawdza:
     * - czy URL używa http zamiast https,
     * - czy domena zawiera podejrzane słowa,
     * - czy URL używa IP zamiast domeny.
     */
    public static Analyzer urlAnalyzer() {

        // Predykat: URL używa nieszyfrowanego http
        Predicate<String> isHttp = url -> url.startsWith("http://");

        // Predykat: URL używa adresu IP
        Predicate<String> hasIpAddress = url ->
                url.matches("https?://\\d{1,3}(\\.\\d{1,3}){3}.*");

        // Predykat: Domena zawiera podejrzane słowa
        Predicate<String> hasSuspiciousDomain = url -> {
            Set<String> suspiciousWords = Set.of(
                    "secure", "login", "verify", "account",
                    "update", "banking", "paypal", "ebay"
            );
            try {
                String host = URI.create(url).getHost();
                if (host == null) return false;
                return suspiciousWords.stream().anyMatch(host::contains);
            } catch (Exception e) {
                return false;
            }
        };

        // Funkcja tworząca Finding dla URL-a na podstawie powodu
        Function<String, Function<String, UrlFinding>> urlFindingBuilder =
                reason -> url -> new UrlFinding(url, reason, 20);

        return email -> email.urls().stream()
                .flatMap(url -> {
                    // Sprawdzamy każdy URL wszystkimi predykatami
                    Stream.Builder<Finding> findings = Stream.builder();

                    if (isHttp.test(url)) {
                        findings.add(urlFindingBuilder
                                .apply("Nieszyfrowane połączenie HTTP")
                                .apply(url));
                    }
                    if (hasIpAddress.test(url)) {
                        findings.add(urlFindingBuilder
                                .apply("URL zawiera adres IP zamiast domeny")
                                .apply(url));
                    }
                    if (hasSuspiciousDomain.test(url)) {
                        findings.add(urlFindingBuilder
                                .apply("Domena zawiera podejrzane słowa kluczowe")
                                .apply(url));
                    }

                    return findings.build();
                })
                .toList();
    }

    // -------------------------------------------------------------------------
    // Punycode
    // -------------------------------------------------------------------------

    /**
     * Reguła wykrywająca domeny zakodowane w Punycode.
     *
     * Punycode (xn--) jest używany do kodowania znaków Unicode w domenach.
     * Ataki homograficzne polegają na zastępowaniu liter podobnymi znakami
     * z innych alfabetów, np. "pаypal.com" (cyrylica 'а' zamiast łacińskiego 'a').
     */
    public static Analyzer punycodeAnalyzer() {

        // Predykat: domena zawiera punycode
        Predicate<String> isPunycode = domain ->
                domain.toLowerCase().contains("xn--");

        // Funkcja wyciągająca domenę z adresu e-mail
        Function<String, String> extractDomain = address -> {
            int at = address.indexOf('@');
            if (at < 0 || at == address.length() - 1) return "";
            return address.substring(at + 1).toLowerCase();
        };

        // Funkcja wyciągająca host z URL
        Function<String, String> extractHost = url -> {
            try {
                String host = URI.create(url).getHost();
                return host != null ? host : "";
            } catch (Exception e) {
                return "";
            }
        };

        return email -> {
            // Sprawdzamy domenę nadawcy
            String senderDomain = extractDomain.apply(email.from());

            Stream<Finding> senderFindings = isPunycode.test(senderDomain)
                    ? Stream.of(new PunycodeFinding(senderDomain, 40))
                    : Stream.empty();

            // Sprawdzamy hosty ze wszystkich URL-i
            Stream<Finding> urlFindings = email.urls().stream()
                    .map(extractHost)
                    .filter(isPunycode)
                    .map(domain -> new PunycodeFinding(domain, 30));

            return Stream.concat(senderFindings, urlFindings).toList();
        };
    }

    // -------------------------------------------------------------------------
    // Metadane / Nagłówki
    // -------------------------------------------------------------------------

    /**
     * Reguła analizująca metadane wiadomości.
     *
     * Sprawdza:
     * - rozbieżność Between From i Reply-To,
     * - podejrzane priorytety (X-Priority: 1),
     * - brak SPF/DKIM w nagłówkach.
     *
     * Pokazuje użycie Optional (orElse, filter, map, ifPresent).
     */
    public static Analyzer metadataAnalyzer() {
        return email -> {
            Stream.Builder<Finding> findings = Stream.builder();

            // Sprawdzenie rozbieżności From vs Reply-To
            // Używamy Optional - bezpieczna obsługa braku danych
            email.senderDomain().ifPresent(senderDomain ->
                    email.replyToDomain()
                            .filter(replyDomain -> !replyDomain.equals(senderDomain))
                            .ifPresent(replyDomain ->
                                    findings.add(new MetadataFinding(
                                            "Reply-To",
                                            replyDomain,
                                            "Domena Reply-To różni się od domeny nadawcy: "
                                                    + senderDomain,
                                            35
                                    ))
                            )
            );

            // Sprawdzenie wysokiego priorytetu (X-Priority: 1)
            email.header("x-priority")
                    .filter("1"::equals)
                    .ifPresent(val ->
                            findings.add(new MetadataFinding(
                                    "X-Priority",
                                    val,
                                    "Wiadomość oznaczona jako najwyższy priorytet",
                                    10
                            ))
                    );

            // Sprawdzenie braku informacji o SPF/DKIM
            boolean hasAuthResults = email.header("authentication-results").isPresent()
                    || email.header("received-spf").isPresent();

            if (!hasAuthResults) {
                findings.add(new MetadataFinding(
                        "Authentication-Results",
                        "brak",
                        "Brak wyników uwierzytelnienia SPF/DKIM",
                        20
                ));
            }

            return findings.build().toList();
        };
    }

    // -------------------------------------------------------------------------
    // Kompozycja - łączenie wszystkich analizatorów w jeden
    // -------------------------------------------------------------------------

    /**
     * Tworzy złożony analizator ze wszystkich dostępnych reguł.
     *
     * Używamy własnej metody andAlso() z interfejsu Analyzer,
     * która jest przykładem kompozycji funkcji.
     */
    public static Analyzer fullAnalyzer() {
        return keywordAnalyzer()
                .andAlso(urlAnalyzer())
                .andAlso(punycodeAnalyzer())
                .andAlso(metadataAnalyzer());
    }

    // -------------------------------------------------------------------------
    // Generyczny builder reguł - własna funkcja wyższego rzędu
    // -------------------------------------------------------------------------

    /**
     * Generyczna metoda tworząca analizator na podstawie:
     * - funkcji ekstrakcji wartości z wiadomości,
     * - predykatu sprawdzającego wartość,
     * - funkcji tworzącej Finding.
     *
     * To jest własna funkcja wyższego rzędu - przyjmuje Function i Predicate
     * jako argumenty i zwraca Analyzer (który też jest funkcją).
     */
    public static <T> Analyzer buildRule(
            Function<EmailMessage, T> extractor,
            Predicate<T> condition,
            Function<T, Finding> findingMapper
    ) {
        return email -> {
            T value = extractor.apply(email);
            return condition.test(value)
                    ? List.of(findingMapper.apply(value))
                    : List.of();
        };
    }

    private AnalyzerFactory() {}
}

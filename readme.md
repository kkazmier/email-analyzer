# 📧 Email Analyzer

System analizy podejrzanych wiadomości e-mail napisany w **Javie 21**
z wykorzystaniem paradygmatu **programowania funkcyjnego**.

---

## 📋 Spis treści

- [Opis projektu](#opis-projektu)
- [Funkcjonalności](#funkcjonalności)
- [Technologie](#technologie)
- [Struktura projektu](#struktura-projektu)
- [Uruchomienie](#uruchomienie)
- [Przykładowy wynik](#przykładowy-wynik)
- [Elementy programowania funkcyjnego](#elementy-programowania-funkcyjnego)

---

## Opis projektu

Email Analyzer to aplikacja analizująca wiadomości e-mail w formacie `.eml`
pod kątem cech charakterystycznych dla **phishingu** i **spamu**.

Aplikacja przetwarza pliki `.eml`, wykrywa podejrzane elementy
i generuje raport z oceną poziomu ryzyka.

---

## Funkcjonalności

### 🔍 Analiza wiadomości
| Moduł | Co sprawdza |
|---|---|
| **Słowa kluczowe** | Wykrywa podejrzane słowa w temacie i treści (`urgent`, `verify`, `password`, `bank`, `login` i inne) |
| **Analiza URL** | Wykrywa nieszyfrowane połączenia HTTP, adresy IP zamiast domen, podejrzane słowa w domenach |
| **Punycode** | Wykrywa domeny zakodowane w Punycode (`xn--`) używane w atakach homograficznych |
| **Metadane** | Sprawdza rozbieżność `From` / `Reply-To`, wysoki priorytet, brak SPF/DKIM |

### 📊 Ocena ryzyka
| Score | Poziom ryzyka |
|---|---|
| 0 - 29 | 🟢 LOW - wiadomość wydaje się bezpieczna |
| 30 - 59 | 🟡 MEDIUM - zachowaj ostrożność |
| 60 - 99 | 🟠 HIGH - prawdopodobny phishing |
| 100+ | 🔴 CRITICAL - bardzo wysokie prawdopodobieństwo ataku |

### 📁 Raporty
- Wyświetlanie wyników na konsoli
- Automatyczny zapis raportów do folderu `reports/`
- Nazwa pliku zawiera adres nadawcy i znacznik czasu

---

## Technologie

- **Java 21**
- **Jakarta Mail 2.0.1** - parsowanie plików `.eml`
- **Maven** - zarządzanie zależnościami

---

## Struktura projektu

```
email-analyzer/
├── mails                           # Przykładowe wiadomości
├── pom.xml
├── reports/                        # Generowane raporty
├── sample-clean.eml                # Przykładowa bezpieczna wiadomość
└── src/
    └── main/
        └── java/
            └── emailanalyzer/
                ├── Main.java
                ├── model/
                │   ├── EmailMessage.java       # Record - model wiadomości
                │   ├── Finding.java            # Sealed interface - wykrycia
                │   ├── KeywordFinding.java     # Record
                │   ├── UrlFinding.java         # Record
                │   ├── MetadataFinding.java    # Record
                │   ├── PunycodeFinding.java    # Record
                │   ├── RiskLevel.java          # Enum poziomów ryzyka
                │   └── AnalysisReport.java     # Record - raport
                ├── parser/
                │   └── EmailParser.java        # Parsowanie .eml
                ├── analyzer/
                │   ├── Analyzer.java           # Funkcyjny interfejs
                │   ├── AnalyzerFactory.java    # Fabryka reguł analizy
                │   └── AnalysisService.java    # Serwis analizy
                └── report/
                    ├── ReportPrinter.java      # Wyświetlanie raportu
                    └── ReportExporter.java     # Zapis raportu do pliku
```

---

## Uruchomienie

### Wymagania
- Java 21+
- Maven 3.8+

### Kroki

**1. Klonowanie repozytorium**
```bash
git clone https://github.com/TwojaNazwa/email-analyzer.git
cd email-analyzer
```

**2. Budowanie projektu**
```bash
mvn compile
```

**3. Uruchomienie**

---

## Przykładowy wynik

```
Znaleziono plików .eml: 1

============================================================
  RAPORT ANALIZY WIADOMOŚCI E-MAIL
============================================================
Od:      security@paypa1-verify.com
Temat:   URGENT: Verify your account immediately!
URL-e:   2
------------------------------------------------------------
WYKRYCIA:
  [SŁOWO KLUCZOWE] waga: 15
    -> Podejrzane słowo kluczowe: 'urgent' znalezione w: subject
  [SŁOWO KLUCZOWE] waga: 15
    -> Podejrzane słowo kluczowe: 'verify' znalezione w: subject
  [URL]            waga: 20
    -> Podejrzany URL: 'http://192.168.1.1/fake-login' - powód: Nieszyfrowane połączenie HTTP
  [URL]            waga: 20
    -> Podejrzany URL: 'http://192.168.1.1/fake-login' - powód: URL zawiera adres IP zamiast domeny
  [PUNYCODE]       waga: 30
    -> Domena zakodowana w Punycode (możliwy atak homograficzny): 'paypal.xn--com-p18d.evil.com'
  [METADANE]       waga: 35
    -> Anomalia w nagłówku 'Reply-To': wartość='totally-different-domain.com'
  ...

------------------------------------------------------------
PODSUMOWANIE:
  MetadataFinding           : 3
  UrlFinding                : 4
  PunycodeFinding           : 1
  KeywordFinding            : 8

Score:        295
Poziom ryzyka: KRYTYCZNY - bardzo wysokie prawdopodobieństwo ataku
============================================================

Raport zapisany: C:\...\email-analyzer\reports\raport_security_paypa1-verify.com_2025-01-15_14-30-22.txt
```

---

## Elementy programowania funkcyjnego

Projekt demonstruje następujące elementy paradygmatu funkcyjnego w Javie:

### 📌 Niemutowalność danych
Wszystkie modele danych są niemutowalne.
Kolekcje zabezpieczone przez `Map.copyOf()` i `List.copyOf()`.
```java
public record EmailMessage(String from, String subject, List<String> urls, ...) {
    public EmailMessage {
        urls    = List.copyOf(urls);
        headers = Map.copyOf(headers);
    }
}
```

### 📌 Własne funkcje wyższego rzędu
`AnalyzerFactory.buildRule()` przyjmuje `Function` i `Predicate` jako argumenty
i zwraca nową funkcję (`Analyzer`).
```java
public static <T> Analyzer buildRule(
        Function<EmailMessage, T> extractor,
        Predicate<T> condition,
        Function<T, Finding> findingMapper
) {
    return email -> condition.test(extractor.apply(email))
            ? List.of(findingMapper.apply(extractor.apply(email)))
            : List.of();
}
```

### 📌 Kompozycja funkcji
Użycie `andThen()` do budowania pipeline'ów przetwarzania tekstu
oraz własna metoda `andAlso()` do kompozycji analizatorów.
```java
Function<String, String> normalize =
        ((Function<String, String>) String::trim)
                .andThen(String::toLowerCase);

Analyzer fullAnalyzer = keywordAnalyzer()
        .andAlso(urlAnalyzer())
        .andAlso(punycodeAnalyzer())
        .andAlso(metadataAnalyzer());
```

### 📌 Stream API
Użycie `map`, `filter`, `reduce`, `collect`, `flatMap`.
```java
int score = findings.stream()
        .mapToInt(Finding::weight)
        .reduce(0, Integer::sum);

Map<String, Long> summary = findings.stream()
        .collect(Collectors.groupingBy(
                f -> f.getClass().getSimpleName(),
                Collectors.counting()
        ));
```

### 📌 Optional
Bezpieczna obsługa wartości opcjonalnych bez `null`.
```java
email.senderDomain().ifPresent(senderDomain ->
        email.replyToDomain()
                .filter(reply -> !reply.equals(senderDomain))
                .ifPresent(reply -> findings.add(...))
);
```

### 📌 Sealed interface
Zamknięta hierarchia typów wykryć - kompilator sprawdza
czy wszystkie przypadki są obsłużone.
```java
public sealed interface Finding
        permits KeywordFinding, UrlFinding, MetadataFinding, PunycodeFinding {
    int weight();
    String description();
}
```

### 📌 Record
Niemutowalne klasy danych z automatycznie generowanymi
konstruktorem, getterami, `equals`, `hashCode`, `toString`.
```java
public record KeywordFinding(String keyword, String foundIn, int weight)
        implements Finding {
    @Override
    public String description() {
        return "Podejrzane słowo kluczowe: '%s' znalezione w: %s"
                .formatted(keyword, foundIn);
    }
}
```

### 📌 Pattern Matching w switch
Dopasowanie typów z dostępem do pól rekordu bez rzutowania.
Kompilator gwarantuje obsługę wszystkich typów `sealed` interfejsu.
```java
String category = switch (finding) {
    case KeywordFinding  k -> "[SŁOWO KLUCZOWE] waga: " + k.weight();
    case UrlFinding      u -> "[URL]            waga: " + u.weight();
    case MetadataFinding m -> "[METADANE]       waga: " + m.weight();
    case PunycodeFinding p -> "[PUNYCODE]       waga: " + p.weight();
};
```

---

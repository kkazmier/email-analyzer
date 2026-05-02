package report;

import model.*;

import java.util.List;
import java.util.Map;

/**
 * Drukowanie raportu na konsolę.
 *
 * Używa pattern matching w switch - kluczowy element projektu.
 * Sealed interface + record + switch = kompletny przykład.
 */
public class ReportPrinter {

    public void print(AnalysisReport report) {
        printHeader(report);
        printFindings(report.findings());
        printSummary(report);
    }

    private void printHeader(AnalysisReport report) {
        System.out.println("=".repeat(60));
        System.out.println("  RAPORT ANALIZY WIADOMOŚCI E-MAIL");
        System.out.println("=".repeat(60));
        System.out.println("Od:      " + report.email().from());
        System.out.println("Temat:   " + report.email().subject());
        System.out.println("URL-e:   " + report.email().urls().size());
        System.out.println("-".repeat(60));
    }

    private void printFindings(List<Finding> findings) {
        if (findings.isEmpty()) {
            System.out.println("  Brak wykryć.");
            return;
        }

        System.out.println("WYKRYCIA:");
        findings.forEach(this::printFinding);
        System.out.println();
    }

    /**
     * Pattern matching w switch - główny element pokazujący
     * użycie sealed interface + record + switch expression.
     *
     * Kompilator sprawdza, że obsługujemy wszystkie typy Finding
     * (bo Finding jest sealed).
     */
    private void printFinding(Finding finding) {
        // Opis bazowy
        String description = finding.description();

        // Pattern matching - dopasowujemy typ i od razu mamy dostęp
        // do pól rekordu bez rzutowania
        String category = switch (finding) {
            case KeywordFinding k  -> "[SŁOWO KLUCZOWE] waga: " + k.weight();
            case UrlFinding u      -> "[URL]            waga: " + u.weight();
            case MetadataFinding m -> "[METADANE]       waga: " + m.weight();
            case PunycodeFinding p -> "[PUNYCODE]       waga: " + p.weight();
        };

        System.out.println("  " + category);
        System.out.println("    -> " + description);
    }

    private void printSummary(AnalysisReport report) {
        System.out.println("-".repeat(60));

        // Podsumowanie typów wykryć - Stream API + Collectors
        Map<String, Long> summary = report.findingsSummary();
        System.out.println("PODSUMOWANIE:");
        summary.forEach((type, count) ->
                System.out.printf("  %-25s : %d%n", type, count)
        );

        System.out.println();
        System.out.println("Score:        " + report.score());

        // Pattern matching dla poziomu ryzyka
        String riskDescription = switch (report.riskLevel()) {
            case LOW      -> "NISKI - wiadomość wydaje się bezpieczna";
            case MEDIUM   -> "ŚREDNI - zachowaj ostrożność";
            case HIGH     -> "WYSOKI - prawdopodobny phishing";
            case CRITICAL -> "KRYTYCZNY - bardzo wysokie prawdopodobieństwo ataku";
        };

        System.out.println("Poziom ryzyka: " + riskDescription);
        System.out.println("=".repeat(60));
    }
}

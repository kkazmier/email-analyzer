package model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Niemutowalny raport z analizy wiadomości.
 */
public record AnalysisReport(
        EmailMessage email,
        List<Finding> findings,
        int score,
        RiskLevel riskLevel
) {

    public AnalysisReport {
        findings = List.copyOf(findings);
    }

    /**
     * Grupuje wykrycia według typu - używamy Stream API + Collectors.
     */
    public Map<String, Long> findingsSummary() {
        return findings.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getClass().getSimpleName(),
                        Collectors.counting()
                ));
    }

    /**
     * Sprawdza, czy raport zawiera jakieś wykrycia.
     */
    public boolean hasFindings() {
        return !findings.isEmpty();
    }
}

package analyzer;

import model.AnalysisReport;
import model.EmailMessage;
import model.Finding;
import model.RiskLevel;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Serwis uruchamiający analizę i budujący raport.
 *
 * Używa Stream API do:
 * - uruchomienia wszystkich analizatorów,
 * - zebrania wyników,
 * - obliczenia score przez reduce.
 */
public class AnalysisService {

    private final Analyzer analyzer;

    /**
     * Konstruktor przyjmuje Analyzer - możemy wstrzyknąć dowolny,
     * co ułatwia testowanie i kompozycję.
     */
    public AnalysisService(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Domyślny konstruktor używający pełnego analizatora.
     */
    public static AnalysisService withDefaultAnalyzers() {
        return new AnalysisService(AnalyzerFactory.fullAnalyzer());
    }

    /**
     * Analizuje wiadomość i zwraca niemutowalny raport.
     *
     * Pipeline:
     * 1. Uruchom analizator -> Lista Finding
     * 2. Oblicz score przez reduce
     * 3. Wyznacz poziom ryzyka
     * 4. Zbuduj raport
     */
    public AnalysisReport analyze(EmailMessage email) {

        // Uruchomienie analizatora - to jest wywołanie złożonej funkcji
        List<Finding> findings = analyzer.apply(email);

        // Funkcja wyciągająca wagę z Finding - używamy do reduce
        ToIntFunction<Finding> getWeight = Finding::weight;

        // Obliczanie score przez reduce - suma wag wszystkich wykryć
        int score = findings.stream()
                .mapToInt(getWeight)
                .reduce(0, Integer::sum);

        // Wyznaczenie poziomu ryzyka
        RiskLevel riskLevel = RiskLevel.fromScore(score);

        return new AnalysisReport(email, findings, score, riskLevel);
    }

    /**
     * Przykład użycia Function do transformacji raportu.
     * Czysta funkcja - nie modyfikuje oryginalnego raportu.
     */
    public <T> T analyzeAndTransform(
            EmailMessage email,
            Function<AnalysisReport, T> transformer
    ) {
        return transformer.apply(analyze(email));
    }
}

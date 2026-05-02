import analyzer.AnalysisService;
import model.AnalysisReport;
import model.EmailMessage;
import parser.EmailParser;
import report.ReportPrinter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Punkt wejścia aplikacji.
 *
 * Pokazuje kompozycję całego pipeline:
 * parse -> analyze -> print
 */
public class Main {

    public static void main(String[] args) {

        // Pobieramy ścieżki plików z argumentów lub używamy przykładowych
        List<Path> paths = Optional.of(args)
                .filter(a -> a.length > 0)
                .map(a -> Arrays.stream(a)
                        .map(Path::of)
                        .toList())
                .orElseGet(() -> List.of(Path.of("sample.eml")));

        // Budujemy pipeline jako złożenie funkcji
        EmailParser parser          = new EmailParser();
        AnalysisService service     = AnalysisService.withDefaultAnalyzers();
        ReportPrinter printer       = new ReportPrinter();

        // Function pipeline: Path -> EmailMessage -> AnalysisReport
        Function<Path, Optional<EmailMessage>> parseStep = path -> {
            try {
                return Optional.of(parser.parse(path));
            } catch (Exception e) {
                System.err.println("Błąd parsowania: " + path + " -> " + e.getMessage());
                return Optional.empty();
            }
        };

        Function<EmailMessage, AnalysisReport> analyzeStep = service::analyze;

        // Uruchamiamy pipeline dla każdego pliku
        paths.stream()
                .map(parseStep)
                .flatMap(Optional::stream)          // pomijamy błędy parsowania
                .map(analyzeStep)
                .forEach(printer::print);
    }
}

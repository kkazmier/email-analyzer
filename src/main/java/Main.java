import analyzer.AnalysisService;
import model.AnalysisReport;
import model.EmailMessage;
import parser.EmailParser;
import report.ReportExporter;
import report.ReportPrinter;

import java.io.IOException;
import java.nio.file.Files;
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

        EmailParser parser       = new EmailParser();
        AnalysisService service  = AnalysisService.withDefaultAnalyzers();
        ReportPrinter printer    = new ReportPrinter();
        ReportExporter exporter  = new ReportExporter();

        Function<Path, Optional<EmailMessage>> parseStep = path -> {
            try {
                return Optional.of(parser.parse(path));
            } catch (Exception e) {
                System.err.println("Błąd parsowania: " + path + " -> " + e.getMessage());
                return Optional.empty();
            }
        };

        Function<EmailMessage, AnalysisReport> analyzeStep = service::analyze;

        // Znajdź pliki .eml
        List<Path> paths = Optional.of(args)
                .filter(a -> a.length > 0)
                .map(a -> Arrays.stream(a)
                        .map(Path::of)
                        .toList())
                .orElseGet(() -> {
                    try {
                        return Files.list(Path.of("./mails"))
                                .filter(p -> p.toString().endsWith(".eml"))
                                .sorted()
                                .toList();
                    } catch (IOException e) {
                        return List.of();
                    }
                });

        System.out.println("Znaleziono plików .eml: " + paths.size());
        System.out.println();

        // Pipeline: parse -> analyze -> print + export
        paths.stream()
                .map(parseStep)
                .flatMap(Optional::stream)
                .map(analyzeStep)
                .forEach(report -> {
                    // Wyświetl na konsolę
                    printer.print(report);

                    // Zapisz do pliku w reports/
                    try {
                        exporter.export(report);
                    } catch (IOException e) {
                        System.err.println("Błąd zapisu raportu: " + e.getMessage());
                    }
                });
    }
}


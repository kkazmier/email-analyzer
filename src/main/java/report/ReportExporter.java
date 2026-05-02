package report;

import model.AnalysisReport;
import model.Finding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ReportExporter {

    /**
     * Ścieżka do folderu z raportami - względem katalogu projektu.
     */
    private static final Path REPORTS_DIR = Path.of("reports");

    /**
     * Format daty do nazwy pliku.
     */
    private static final DateTimeFormatter FILENAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Eksportuje raport do folderu reports/ z automatyczną nazwą pliku.
     * Nazwa pliku zawiera temat wiadomości i datę.
     */
    public Path export(AnalysisReport report) throws IOException {
        ensureDirectoryExists();

        String fileName = generateFileName(report);
        Path outputPath = REPORTS_DIR.resolve(fileName);

        String content = buildContent(report);
        Files.writeString(outputPath, content);

        System.out.println("Raport zapisany: " + outputPath.toAbsolutePath());
        return outputPath;
    }

    /**
     * Eksportuje raport pod konkretną nazwą w folderze reports/.
     */
    public Path export(AnalysisReport report, String fileName) throws IOException {
        ensureDirectoryExists();

        Path outputPath = REPORTS_DIR.resolve(fileName);

        String content = buildContent(report);
        Files.writeString(outputPath, content);

        System.out.println("Raport zapisany: " + outputPath.toAbsolutePath());
        return outputPath;
    }

    /**
     * Tworzy folder reports/ jeśli nie istnieje.
     */
    private void ensureDirectoryExists() throws IOException {
        if (!Files.exists(REPORTS_DIR)) {
            Files.createDirectories(REPORTS_DIR);
            System.out.println("Utworzono folder: " + REPORTS_DIR.toAbsolutePath());
        }
    }

    /**
     * Generuje nazwę pliku na podstawie nadawcy i daty.
     * Np. "raport_security_paypa1-verify.com_2024-01-15_14-30-22.txt"
     */
    private String generateFileName(AnalysisReport report) {
        String sender = report.email().from()
                .replaceAll("[^a-zA-Z0-9.@_-]", "")  // usuń niedozwolone znaki
                .replace("@", "_");

        String timestamp = LocalDateTime.now().format(FILENAME_FORMAT);

        return "raport_%s_%s.txt".formatted(sender, timestamp);
    }

    private String buildContent(AnalysisReport report) {
        String header = """
                ============================================================
                  RAPORT ANALIZY WIADOMOŚCI E-MAIL
                ============================================================
                Od:      %s
                Temat:   %s
                URL-e:   %d
                ------------------------------------------------------------
                Score:   %d
                Ryzyko:  %s
                ============================================================
                
                WYKRYCIA (%d):
                """.formatted(
                report.email().from(),
                report.email().subject(),
                report.email().urls().size(),
                report.score(),
                report.riskLevel(),
                report.findings().size()
        );

        String findings = report.findings().stream()
                .map(Finding::description)
                .collect(Collectors.joining("\n  - ", "  - ", "\n"));

        String summary = """
                
                PODSUMOWANIE:
                %s
                """.formatted(
                report.findingsSummary().entrySet().stream()
                        .map(e -> "  %-25s : %d".formatted(e.getKey(), e.getValue()))
                        .collect(Collectors.joining("\n"))
        );

        return header + findings + summary;
    }
}

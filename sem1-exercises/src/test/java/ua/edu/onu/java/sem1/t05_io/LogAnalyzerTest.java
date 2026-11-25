package ua.edu.onu.java.sem1.t05_io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void countsLevelsAndWritesSortedSummary() throws IOException {
        Path input = tempDir.resolve("app.log");
        Files.writeString(input, """
                INFO: started
                error: db timeout
                WARN: slow query
                invalid line without colon
                
                INFO: request handled
                ERROR: disk full
                """);

        Map<String, Integer> counts = LogAnalyzer.countByLevel(input);
        assertEquals(2, counts.get("INFO"));
        assertEquals(1, counts.get("WARN"));
        assertEquals(2, counts.get("ERROR"));
        assertEquals(3, counts.size());

        Path output = tempDir.resolve("summary.txt");
        LogAnalyzer.writeSummary(counts, output);
        assertEquals(List.of("ERROR=2", "INFO=2", "WARN=1"), Files.readAllLines(output));
    }
}

package lv.bootcamp.shelter.task5;

import lv.bootcamp.shelter.model.Animal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 5: Nested test classes for CSV parsing
 * <p>
 * Practice:
 * - @Nested to organize by scenario
 * - @DisplayName for readable output
 * - Testing Optional results (isPresent, isEmpty)
 * - Testing file I/O with temp files
 * <p>
 * Instructions:
 * Write tests for AnimalCsvParser. Use @Nested classes to group tests by scenario.
 * For file-based tests, use Files.createTempFile() and Files.writeString() to create test data.
 */
@DisplayName("AnimalCsvParser")
class AnimalCsvParserTest {

    private AnimalCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new AnimalCsvParser();
    }

    // ==================== parseRow tests ====================

    @Nested
    @DisplayName("When parsing valid rows")
    class ValidRows {

        @Test
        @DisplayName("parses a complete row into an Animal")
        void shouldParseCompleteRow() {
            Optional<Animal> opt = parser.parseRow("Buddy,Dog,3,true,2026-01-15");
            assertTrue(opt.isPresent());
            assertEquals("Buddy", opt.get().getName());
            assertEquals(3, opt.get().getAge());
        }

        @Test
        @DisplayName("trims whitespace from fields")
        void shouldTrimWhitespace() {
            Optional<Animal> opt = parser.parseRow("  Buddy , Dog , 3 , true , 2026-01-15 ");
            assertTrue(opt.isPresent());
            assertEquals("Buddy", opt.get().getName());
        }

        @Test
        @DisplayName("parses vaccinated=false correctly")
        void shouldParseFalseVaccination() {
            Optional<Animal> opt = parser.parseRow("Buddy,Dog,3,false,2026-01-15");
            assertTrue(opt.isPresent());
            assertFalse(opt.get().isVaccinated());
        }
    }

    @Nested
    @DisplayName("When parsing malformed rows")
    class MalformedRows {

        @Test
        @DisplayName("returns empty for null input")
        void shouldReturnEmptyForNull() {
            assertTrue(parser.parseRow(null).isEmpty());
        }

        @Test
        @DisplayName("returns empty for blank input")
        void shouldReturnEmptyForBlank() {
            assertTrue(parser.parseRow("   ").isEmpty());
        }

        @Test
        @DisplayName("returns empty when row has fewer than 5 fields")
        void shouldReturnEmptyForTooFewFields() {
            assertTrue(parser.parseRow("Buddy,Dog,3").isEmpty());
        }

        @Test
        @DisplayName("returns empty when name is missing")
        void shouldReturnEmptyForMissingName() {
            assertTrue(parser.parseRow(",Dog,3,true,2026-01-15").isEmpty());
        }

        @Test
        @DisplayName("returns empty when age is not a number")
        void shouldReturnEmptyForBadAge() {
            assertTrue(parser.parseRow("Buddy,Dog,old,true,2026-01-15").isEmpty());
        }

        @Test
        @DisplayName("returns empty when age is negative")
        void shouldReturnEmptyForNegativeAge() {
            assertTrue(parser.parseRow("Buddy,Dog,-1,true,2026-01-15").isEmpty());
        }

        @Test
        @DisplayName("returns empty when date is invalid")
        void shouldReturnEmptyForBadDate() {
            assertTrue(parser.parseRow("Buddy,Dog,3,true,not-a-date").isEmpty());
        }
    }

    @Nested
    @DisplayName("When handling edge cases")
    class EdgeCases {

        @Test
        @DisplayName("handles vaccinated field as any non-true string → false")
        void shouldTreatNonTrueAsFalse() {
            Optional<Animal> opt = parser.parseRow("Buddy,Dog,3,maybe,2026-01-15");
            assertTrue(opt.isPresent());
            assertFalse(opt.get().isVaccinated());
        }

        @Test
        @DisplayName("handles age 0 as valid")
        void shouldAcceptAgeZero() {
            Optional<Animal> opt = parser.parseRow("Buddy,Dog,0,true,2026-01-15");
            assertTrue(opt.isPresent());
            assertEquals(0, opt.get().getAge());
        }
    }

    // ==================== parseFile tests ====================

    @Nested
    @DisplayName("When parsing a CSV file")
    class ParseFile {

        @Test
        @DisplayName("parses valid rows and counts skipped rows")
        void shouldParseFileAndCountSkipped() throws IOException {
            Path tempFile = Files.createTempFile("test-intake", ".csv");
            String content = "name,species,age,vaccinated,intakeDate\n" +
                    "Buddy,Dog,3,true,2026-01-15\n" +
                    "Luna,Cat,2,true,2026-01-16\n" +
                    "Max,Dog,bad_age,false,2026-01-18\n" +
                    "Bella,Cat,1,true,2026-01-19\n";

            Files.writeString(tempFile, content, StandardCharsets.UTF_8);

            AnimalCsvParser.ParseResult result = parser.parseFile(tempFile);

            assertEquals(3, result.animals().size());
            assertEquals(1, result.skippedRows());

            Files.deleteIfExists(tempFile);
        }

        @Test
        @DisplayName("returns empty result for file with only a header")
        void shouldReturnEmptyForHeaderOnly() throws IOException {
            Path tempFile = Files.createTempFile("test-header", ".csv");
            Files.writeString(tempFile, "name,species,age,vaccinated,intakeDate\n", StandardCharsets.UTF_8);
            AnimalCsvParser.ParseResult result = parser.parseFile(tempFile);
            assertTrue(result.animals().isEmpty());
            assertEquals(0, result.skippedRows());
            Files.deleteIfExists(tempFile);
        }

        @Test
        @DisplayName("throws IOException for non-existent file")
        void shouldThrowForMissingFile() {
            assertThrows(IOException.class, () -> parser.parseFile(Path.of("does-not-exist.csv")));
        }
    }
}

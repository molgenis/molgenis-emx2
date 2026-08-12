package org.molgenis.emx2.fairmapper.cli.commands;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import picocli.CommandLine;

class GenerateQueryTest {

  private Schema schema;
  private String expectedQuery;

  private final PrintStream originalOut = System.out;
  private ByteArrayOutputStream capturedOut;

  @BeforeEach
  void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(getClass().getSimpleName());
    Table table =
        schema.create(
            TableMetadata.table(
                "Simple",
                Column.column("name")
                    .setType(ColumnType.STRING)
                    .setPkey()
                    .setSemantics("xsd:name")));
    expectedQuery = new TableQueryGenerator().generate(table.getMetadata());

    capturedOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOut));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void shouldPrintGeneratedQueryToStdout() {
    int exitCode = new CommandLine(new GenerateQuery()).execute(schema.getName(), "Simple");

    assertEquals(0, exitCode);
    assertEquals(expectedQuery + System.lineSeparator(), capturedOut.toString());
  }

  @Test
  void shouldWriteGeneratedQueryToOutputFile(@TempDir Path tempDir) throws IOException {
    Path outputFile = tempDir.resolve("query.sparql");

    int exitCode =
        new CommandLine(new GenerateQuery())
            .execute(schema.getName(), "Simple", "-o", outputFile.toString());

    assertEquals(0, exitCode);
    assertEquals(expectedQuery, Files.readString(outputFile));
  }

  @Test
  void shouldThrowWhenSchemaNotFound() {
    GenerateQuery command = new GenerateQuery();
    new CommandLine(command).parseArgs("does-not-exist-schema", "Simple");

    MolgenisException exception = assertThrows(MolgenisException.class, command::run);
    assertTrue(exception.getMessage().contains("does-not-exist-schema"));
  }

  @Test
  void shouldThrowWhenTableNotFound() {
    GenerateQuery command = new GenerateQuery();
    new CommandLine(command).parseArgs(schema.getName(), "does-not-exist-table");

    assertThrows(MolgenisException.class, command::run);
  }
}

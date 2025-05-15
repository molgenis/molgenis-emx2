package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Execution(SAME_THREAD)
public class RdfStreamTest {
  static Database database;
  static Schema schema;
  static Table table;
  static String baseURL = "http://example.com/streamTest/api/rdf";

  @BeforeAll
  static void beforeAll() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema("streamTest");
    schema.create(
        table(
            "myTable",
            column("id")
                .setType(ColumnType.INT)
                .setPkey()
                .setSemantics("http://purl.org/dc/terms/identifier"),
            column("description")
                .setType(ColumnType.STRING)
                .setSemantics("http://purl.org/dc/terms/title")));
    table = schema.getTable("myTable");

    IntStream.rangeClosed(1, 10000)
        .forEach(i -> table.insert(row("id", i, "description", "description of " + i)));
  }

  @Test
  void compareMethods() throws IOException {
    List<Duration> builderDurations = new ArrayList<>();
    List<Duration> writerDurations = new ArrayList<>();
    List<Duration> streamDurations = new ArrayList<>();

    for (Integer i : IntStream.range(0, 5).boxed().toList()) {
      // baseline
      OutputStream outBuilder = new ByteArrayOutputStream();
      Instant startBuilder = Instant.now();
      processWithBuilder(outBuilder);
      outBuilder.flush();
      builderDurations.add(Duration.between(startBuilder, Instant.now()));

      // rdf4j writer
      OutputStream outWriter = new ByteArrayOutputStream();
      Instant startWriter = Instant.now();
      processWithWriter(outWriter);
      outWriter.flush();
      streamDurations.add(Duration.between(startWriter, Instant.now()));

      // direct to stream
      OutputStream outStream = new ByteArrayOutputStream();
      Instant startStream = Instant.now();
      processWithStream(outStream);
      outStream.flush();
      writerDurations.add(Duration.between(startStream, Instant.now()));

      assertEquals(outBuilder.toString(), outWriter.toString());
      assertEquals(outBuilder.toString(), outStream.toString());
    }

    System.out.println("builder durations: " + builderDurations);
    System.out.println("writer durations: " + writerDurations);
    System.out.println("stream durations: " + streamDurations);
  }

  void processWithBuilder(OutputStream out) {
    ModelBuilder builder = new ModelBuilder();
    for (Row row : table.query().retrieveRows()) {
      final IRI subject = rowIRI(baseURL, table, row);
      for (Column column : table.getMetadata().getColumns()) {
        String[] semantics = column.getSemantics();
        if (semantics != null) {
          // only uses first semantic (keep test simple)
          IRI predicate = Values.iri(column.getSemantics()[0]);
          Literal object = Values.literal(row.getString(column.getName()));
          builder.add(subject, predicate, object);
        }
      }
    }
    Rio.write(builder.build(), out, RDFFormat.TURTLE);
  }

  void processWithWriter(OutputStream out) {
    RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
    SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();

    writer.startRDF();
    for (Row row : table.query().retrieveRows()) {
      final IRI subject = rowIRI(baseURL, table, row);
      for (Column column : table.getMetadata().getColumns()) {
        String[] semantics = column.getSemantics();
        if (semantics != null) {
          // only uses first semantic (keep test simple)
          IRI predicate = Values.iri(column.getSemantics()[0]);
          Literal object = Values.literal(row.getString(column.getName()));
          writer.handleStatement(valueFactory.createStatement(subject, predicate, object));
        }
      }
    }
    writer.endRDF();
  }

  // todo: finish wip
  void processWithStream(OutputStream out) throws IOException {
    // namespaces
    StringBuilder builder = new StringBuilder();

    for (Row row : table.query().retrieveRows()) {
      final IRI subject = rowIRI(baseURL, table, row);
      builder.append("<").append(subject).append(">");
      for (Column column : table.getMetadata().getColumns()) {
        String[] semantics = column.getSemantics();
        if (semantics != null) {
          // only uses first semantic (keep test simple)
          IRI predicate = Values.iri(column.getSemantics()[0]);
          out.write(predicate.stringValue().getBytes());
          Literal object = Values.literal(row.getString(column.getName()));
          out.write(object.stringValue().getBytes());
        }
      }
    }
  }
}

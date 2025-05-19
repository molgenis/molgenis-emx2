package org.molgenis.emx2.rdf;

import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
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

    IntStream.rangeClosed(1, 100000)
        .forEach(i -> table.insert(row("id", i, "description", "description of " + i)));
  }

  @Test
  void compareMethodsOutput() throws IOException {
    ByteArrayOutputStream rdf4jBuilderOutputStream = new ByteArrayOutputStream();
    ByteArrayOutputStream rdf4jWriterOutputStream = new ByteArrayOutputStream();
    ByteArrayOutputStream jenaStreamWriterOutputStream = new ByteArrayOutputStream();
    ByteArrayOutputStream manualStreamOutputStream = new ByteArrayOutputStream();

    processWithRdf4jBuilder(rdf4jBuilderOutputStream);
    processWithRdf4jWriter(rdf4jWriterOutputStream);
    processWithJenaStreamWriter(jenaStreamWriterOutputStream);
    processWithManualStream(manualStreamOutputStream);

    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    Model rdf4jBuilderModel =
        Rio.parse(
            new InputStreamReader(new ByteArrayInputStream(rdf4jBuilderOutputStream.toByteArray())),
            RDFFormat.TURTLE);
    Model rdf4jWriterModel =
        Rio.parse(
            new InputStreamReader(new ByteArrayInputStream(rdf4jWriterOutputStream.toByteArray())),
            RDFFormat.TURTLE);
    Model jenaStreamWriterModel =
        Rio.parse(
            new InputStreamReader(
                new ByteArrayInputStream(jenaStreamWriterOutputStream.toByteArray())),
            RDFFormat.TURTLE);
    Model manualStreamModel =
        Rio.parse(
            new InputStreamReader(new ByteArrayInputStream(manualStreamOutputStream.toByteArray())),
            RDFFormat.TURTLE);

    assertAll(
        () -> assertEquals(rdf4jBuilderModel, rdf4jBuilderModel),
        () -> assertEquals(rdf4jBuilderModel, jenaStreamWriterModel),
        () -> assertEquals(rdf4jBuilderModel, manualStreamModel));
    ;
  }

  @Test
  void compareMethodsUsage() throws IOException {
    List<Duration> rdf4jBuildersDuration = new ArrayList<>();
    List<Duration> rdf4jWritersDuration = new ArrayList<>();
    List<Duration> jenaStreamWritersDuration = new ArrayList<>();
    List<Duration> manualStreamsDuration = new ArrayList<>();

    List<Long> rdf4jBuilderMemory = new ArrayList<>();
    List<Long> rdf4jWriterMemory = new ArrayList<>();
    List<Long> jenaStreamWriterMemory = new ArrayList<>();
    List<Long> manualStreamMemory = new ArrayList<>();

    List<Integer> runs = IntStream.range(0, 5).boxed().toList();

    try (OutputStream nullOutputStream = OutputStream.nullOutputStream()) {
      runs.forEach(
          i ->
              run(
                  RdfStreamTest::processWithRdf4jBuilder,
                  rdf4jBuildersDuration,
                  rdf4jBuilderMemory,
                  nullOutputStream));

      runs.forEach(
          i ->
              run(
                  RdfStreamTest::processWithRdf4jWriter,
                  rdf4jWritersDuration,
                  rdf4jWriterMemory,
                  nullOutputStream));

      runs.forEach(
          i ->
              run(
                  RdfStreamTest::processWithJenaStreamWriter,
                  jenaStreamWritersDuration,
                  jenaStreamWriterMemory,
                  nullOutputStream));

      runs.forEach(
          i ->
              run(
                  RdfStreamTest::processWithManualStream,
                  manualStreamsDuration,
                  manualStreamMemory,
                  nullOutputStream));
    }

    out.println("rdf4j builder durations: " + rdf4jBuildersDuration);
    out.println("rdf4j writer durations: " + rdf4jWritersDuration);
    out.println("jena stream writer durations: " + jenaStreamWritersDuration);
    out.println("manual stream durations: " + manualStreamsDuration);
    out.println("-------- -------- --------");
    out.println("rdf4j builder memory: " + byteToMegaByte(rdf4jBuilderMemory));
    out.println("rdf4j writer memory: " + byteToMegaByte(rdf4jWriterMemory));
    out.println("jena stream writer memory: " + byteToMegaByte(jenaStreamWriterMemory));
    out.println("manual stream memory: " + byteToMegaByte(manualStreamMemory));
  }

  void run(
      Function<OutputStream, Long> function,
      List<Duration> durationList,
      List<Long> memoryList,
      OutputStream out) {
    Instant startTime = Instant.now();
    Long memory = function.apply(out);
    durationList.add(Duration.between(startTime, Instant.now()));
    memoryList.add(memory);
  }

  /** This is vague at best due to influences like GC collection. */
  static Long currentMemoryUsed() {
    System.gc();
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  }

  static List<String> byteToMegaByte(List<Long> bytes) {
    return bytes.stream().map(i -> Long.toString(i / 1024 / 1024) + " MB").toList();
  }

  static Long processWithRdf4jBuilder(OutputStream out) {
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

    Model model = builder.build();
    Long memoryUsed = RdfStreamTest.currentMemoryUsed();
    Rio.write(model, out, RDFFormat.TURTLE);
    return memoryUsed;
  }

  static Long processWithRdf4jWriter(OutputStream out) {
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

    Long memoryUsed = RdfStreamTest.currentMemoryUsed();
    writer.endRDF();
    return memoryUsed;
  }

  static Long processWithJenaStreamWriter(OutputStream out) {
    StreamRDF streamRdf = StreamRDFWriter.getWriterStream(out, Lang.TURTLE);
    streamRdf.start();

    for (Row row : table.query().retrieveRows()) {
      final Node subject = NodeFactory.createURI(rowIRI(baseURL, table, row).stringValue());
      for (Column column : table.getMetadata().getColumns()) {
        String[] semantics = column.getSemantics();
        if (semantics != null) {
          // only uses first semantic (keep test simple)
          Node predicate = NodeFactory.createURI(column.getSemantics()[0]);
          Node object = NodeFactory.createLiteralString(row.getString(column.getName()));
          streamRdf.triple(Triple.create(subject, predicate, object));
        }
      }
    }

    Long memoryUsed = RdfStreamTest.currentMemoryUsed();
    streamRdf.finish();
    return memoryUsed;
  }

  // todo: finish wip
  static Long processWithManualStream(OutputStream out) {
    // missing namespace handling
    // very ugly, always creates full triples (real implementation should be better)

    for (Row row : table.query().retrieveRows()) {
      final IRI subject = rowIRI(baseURL, table, row);
      for (Column column : table.getMetadata().getColumns()) {
        String[] semantics = column.getSemantics();
        if (semantics != null) {
          // only uses first semantic (keep test simple)
          IRI predicate = Values.iri(column.getSemantics()[0]);
          Literal object = Values.literal(row.getString(column.getName()));
          try {
            StringBuilder builder = new StringBuilder();
            builder
                .append("<")
                .append(subject)
                .append("> <")
                .append(predicate)
                .append("> ")
                .append(object)
                .append(" .\n");
            out.write(builder.toString().getBytes());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return RdfStreamTest.currentMemoryUsed();
  }
}

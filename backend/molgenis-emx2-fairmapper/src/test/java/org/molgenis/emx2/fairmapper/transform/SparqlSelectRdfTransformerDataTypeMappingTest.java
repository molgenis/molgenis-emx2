package org.molgenis.emx2.fairmapper.transform;

import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.SemanticTestUtils.toSemantic;

import java.util.Arrays;
import java.util.List;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;

class SparqlSelectRdfTransformerDataTypeMappingTest {

  private static final String TABLE = "Entity";
  private static final IRI SUBJECT = iri("https://example.com/subject/1");
  private static final String PREDICATE_BASE = "https://example.com/predicate/";

  @Test
  void shouldMapStringLiteralAsLabel() {
    IRI predicate = iri(PREDICATE_BASE + "name");
    SchemaMetadata schema = setupSchema(Column.column("name").setSemantics(toSemantic(predicate)));

    Row row = transformAndGetFirstRow(schema, repositoryWith(predicate, literal("Alice")));

    assertEquals("Alice", row.getString("name"));
  }

  @Test
  void shouldMapIntegerLiteralWithTypeAnnotation() {
    IRI predicate = iri(PREDICATE_BASE + "age");
    SchemaMetadata schema =
        setupSchema(
            Column.column("age").setType(ColumnType.INT).setSemantics(toSemantic(predicate)));

    Row row = transformAndGetFirstRow(schema, repositoryWith(predicate, literal(42)));

    assertEquals("42", row.getString("age"));
    assertEquals(42, row.getInteger("age"));
  }

  @Test
  void shouldMapDecimalLiteralWithTypeAnnotation() {
    IRI predicate = iri(PREDICATE_BASE + "price");
    SchemaMetadata schema =
        setupSchema(
            Column.column("price").setType(ColumnType.DECIMAL).setSemantics(toSemantic(predicate)));

    Row row = transformAndGetFirstRow(schema, repositoryWith(predicate, literal(1.5d)));

    // EMX2 DECIMAL is stored as XSD double in RDF; stringValue() returns the lexical label
    assertEquals("1.5", row.getString("price"));
    assertEquals(1.5, row.getDecimal("price"));
  }

  @Test
  void shouldMapFloatLiteralWithTypeAnnotation() {
    IRI predicate = iri(PREDICATE_BASE + "weight");
    SchemaMetadata schema =
        setupSchema(Column.column("weight").setSemantics(toSemantic(predicate)));

    Row row = transformAndGetFirstRow(schema, repositoryWith(predicate, literal(1.5f)));

    assertEquals("1.5", row.getString("weight"));
  }

  @Test
  void shouldMapIriValueAsPlainIriString() {
    IRI predicate = iri(PREDICATE_BASE + "related");
    IRI iriValue = iri("https://example.com/other/42");
    SchemaMetadata schema =
        setupSchema(Column.column("related").setSemantics(toSemantic(predicate)));

    Row row = transformAndGetFirstRow(schema, repositoryWith(predicate, iriValue));

    assertEquals("https://example.com/other/42", row.getString("related"));
  }

  @Test
  void shouldMapStringArrayValuesAsCommaSeparated() {
    IRI predicate = iri(PREDICATE_BASE + "tags");
    SchemaMetadata schema =
        setupSchema(
            Column.column("tags")
                .setType(ColumnType.STRING_ARRAY)
                .setSemantics(toSemantic(predicate)));

    Row row =
        transformAndGetFirstRow(
            schema, repositoryWithMultiple(predicate, literal("alpha"), literal("beta")));

    List<String> parts = Arrays.asList(row.getString("tags").split("\\|"));
    assertEquals(2, parts.size());
    assertTrue(parts.contains("alpha"));
    assertTrue(parts.contains("beta"));
  }

  @Test
  void shouldMapIntegerArrayValuesAsCommaSeparatedLexicalValues() {
    IRI predicate = iri(PREDICATE_BASE + "scores");
    SchemaMetadata schema =
        setupSchema(
            Column.column("scores")
                .setType(ColumnType.INT_ARRAY)
                .setSemantics(toSemantic(predicate)));

    Row row =
        transformAndGetFirstRow(
            schema, repositoryWithMultiple(predicate, literal(10), literal(20)));

    // STR() in GROUP_CONCAT strips the XSD type annotation, leaving just the lexical value
    List<String> parts = Arrays.asList(row.getString("scores").split("\\|"));
    assertEquals(2, parts.size());
    assertTrue(parts.contains("10"));
    assertTrue(parts.contains("20"));
  }

  private SailRepository repositoryWith(IRI predicate, Value object) {
    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection conn = repository.getConnection()) {
      conn.add(statement(SUBJECT, predicate, object, null));
      conn.commit();
    }
    return repository;
  }

  private SailRepository repositoryWithMultiple(IRI predicate, Value... objects) {
    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection conn = repository.getConnection()) {
      for (Value obj : objects) {
        conn.add(statement(SUBJECT, predicate, obj, null));
      }
      conn.commit();
    }
    return repository;
  }

  private SchemaMetadata setupSchema(Column... columns) {
    SchemaMetadata schema = new SchemaMetadata(getClass().getSimpleName());
    schema.create(TableMetadata.table(TABLE, columns));
    return schema;
  }

  private Row transformAndGetFirstRow(SchemaMetadata schema, SailRepository repo) {
    SparqlSelectRdfTransformer transformer =
        new SparqlSelectRdfTransformer(new TableQueryGenerator(), schema, List.of(TABLE));
    TableStore store = transformer.transform(repo);
    return store.readTable(TABLE).iterator().next();
  }
}

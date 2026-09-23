package org.molgenis.emx2.rdf.generators.query.generators;

import static org.molgenis.emx2.rdf.generators.query.generators.SparqlQueryTestUtils.*;
import static org.molgenis.emx2.rdf.generators.query.generators.SparqlQueryTestUtils.assertHasResults;

import java.util.Map;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;

class ReferenceColumnSparqlQueryGeneratorIntegrationTest {

  private static final String SCHEMA_NAME =
      ReferenceColumnSparqlQueryGenerator.class.getSimpleName();

  private static final String PRODUCT_IRI = "https://example.com/product";
  private static final String ORDER_IRI = "https://example.com/order";
  private static final String MANUFACTURER_IRI = "https://example.com/manufacturer";
  public static final TableQueryGenerator GENERATOR = new TableQueryGenerator();

  @Test
  void givenReference_thenOnlyUseKey() {
    SailRepository repository =
        repository(
            statement(PRODUCT_IRI, DCTERMS.TITLE, "pet"),
            statement(ORDER_IRI, DCTERMS.IDENTIFIER, "order1"),
            statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI)),
            statement(ORDER_IRI + 2, DCTERMS.IDENTIFIER, "order2"));

    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA_NAME)
            .create(productTableWithSemantics("dcterms:title"), orderTable(true));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(schema.getTableMetadata("Order"));
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
          bindingSets,
          Map.of(
              SparqlVariableUtil.SUBJECT_NAME,
              ORDER_IRI,
              "product__name",
              "pet",
              SparqlVariableUtil.SUBJECT_NAME + "product",
              PRODUCT_IRI,
              "id",
              "order1"));
    }
  }

  @Test
  void givenReference_whenPrimaryKeyHasNoSemantics_thenSkip() {
    SailRepository repository =
        repository(
            statement(PRODUCT_IRI, DCTERMS.TITLE, "pet"),
            statement(ORDER_IRI, DCTERMS.IDENTIFIER, "order1"),
            statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI)));

    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA_NAME).create(productTableWithSemantics(), orderTable(true));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(schema.getTableMetadata("Order"));
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
          bindingSets,
          Map.of(
              SparqlVariableUtil.SUBJECT_NAME,
              ORDER_IRI,
              SparqlVariableUtil.SUBJECT_NAME + "product",
              PRODUCT_IRI,
              "id",
              "order1"));
    }
  }

  @Test
  void givenReference_whenOptional_thenAddOptional() {
    SailRepository repository =
        repository(
            statement(PRODUCT_IRI, DCTERMS.TITLE, "pet"),
            statement(ORDER_IRI, DCTERMS.IDENTIFIER, "order1"),
            statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI)),
            statement(ORDER_IRI + 2, DCTERMS.IDENTIFIER, "order2"));

    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA_NAME)
            .create(productTableWithSemantics("dcterms:title"), orderTable(false));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(schema.getTableMetadata("Order"));
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
          bindingSets,
          Map.of(
              SparqlVariableUtil.SUBJECT_NAME,
              ORDER_IRI,
              SparqlVariableUtil.SUBJECT_NAME + "product",
              PRODUCT_IRI,
              "product__name",
              "pet",
              "id",
              "order1"),
          Map.of(SparqlVariableUtil.SUBJECT_NAME, ORDER_IRI + 2, "id", "order2"));
    }
  }

  @Test
  void givenReference_whenCompositeKey_thenUseAllKeys() {
    SailRepository repository =
        repository(
            statement(PRODUCT_IRI, DCTERMS.TITLE, "pet"),
            statement(PRODUCT_IRI, DCTERMS.DESCRIPTION, "description"),
            statement(ORDER_IRI, DCTERMS.IDENTIFIER, "order1"),
            statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI)));

    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA_NAME)
            .create(
                productTableWithSemantics("dcterms:title")
                    .add(
                        Column.column("description").setSemantics("dcterms:description").setPkey()),
                orderTable(true));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(schema.getTableMetadata("Order"));
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
          bindingSets,
          Map.of(
              SparqlVariableUtil.SUBJECT_NAME,
              ORDER_IRI,
              SparqlVariableUtil.SUBJECT_NAME + "product",
              PRODUCT_IRI,
              "product__name",
              "pet",
              "product__description",
              "description",
              "id",
              "order1"));
    }
  }

  @Test
  void givenReference_whenCompositeKeyPartIsReference_thenUseAllSubkeys() {
    SailRepository repository =
        repository(
            statement(MANUFACTURER_IRI, FOAF.FIRST_NAME, "Beau"),
            statement(MANUFACTURER_IRI, FOAF.LAST_NAME, "ter Ham"),
            statement(PRODUCT_IRI, DCTERMS.TITLE, "pet"),
            statement(PRODUCT_IRI, DCTERMS.CREATOR, Values.iri(MANUFACTURER_IRI)),
            statement(ORDER_IRI, DCTERMS.IDENTIFIER, "order1"),
            statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI)));

    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA_NAME)
            .create(
                TableMetadata.table(
                    "Manufacturer",
                    Column.column("firstName")
                        .setType(ColumnType.STRING)
                        .setPkey()
                        .setSemantics("foaf:firstName"),
                    Column.column("lastName")
                        .setType(ColumnType.STRING)
                        .setPkey()
                        .setSemantics("foaf:lastName")),
                productTableWithSemantics("dcterms:title")
                    .add(
                        Column.column("manufacturer")
                            .setType(ColumnType.REF)
                            .setPkey()
                            .setRefTable("Manufacturer")
                            .setSemantics("dcterms:creator")),
                orderTable(true));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(schema.getTableMetadata("Order"));
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
          bindingSets,
          Map.of(
              SparqlVariableUtil.SUBJECT_NAME,
              ORDER_IRI,
              SparqlVariableUtil.SUBJECT_NAME + "product",
              PRODUCT_IRI,
              "product__name",
              "pet",
              "_subject_product__manufacturer",
              MANUFACTURER_IRI,
              "product__manufacturer__firstName",
              "Beau",
              "product__manufacturer__lastName",
              "ter Ham",
              "id",
              "order1"));
    }
  }

  @Test
  void shouldResolveCrossSchemaReferences() {
    SailRepository repository =
            repository(
                    statement(PRODUCT_IRI, DCTERMS.TITLE, "pet"),
                    statement(ORDER_IRI, DCTERMS.IDENTIFIER, "order1"),
                    statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI)),
                    statement(ORDER_IRI + 2, DCTERMS.IDENTIFIER, "order2"));

    SchemaMetadata schemaB = new SchemaMetadata(SCHEMA_NAME + "B");
    schemaB.create(productTableWithSemantics("dcterms:title"));

    SchemaMetadata schemaA = new SchemaMetadata(SCHEMA_NAME + "A");
    TableMetadata order = orderTable(true);
    order.getColumn("product").setRefSchemaName(schemaB.getName());
    schemaA.create(order);

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(schemaA.getTableMetadata("Order"));
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
              bindingSets,
              Map.of(
                      SparqlVariableUtil.SUBJECT_NAME,
                      ORDER_IRI,
                      "product__name",
                      "pet",
                      SparqlVariableUtil.SUBJECT_NAME + "product",
                      PRODUCT_IRI,
                      "id",
                      "order1"));
    }
  }

  @Nested
  class ReferenceArrayTest {

    @Test
    void givenArrayReference_whenSemanticsEmpty_thenOnlySelectSubjectId() {
      SailRepository repository =
          repository(
              statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI)),
              statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI + 2)),
              statement(PRODUCT_IRI + 2, DCTERMS.TITLE, "cat"),
              statement(PRODUCT_IRI, DCTERMS.TITLE, "dog"),
              statement(ORDER_IRI, DCTERMS.IDENTIFIER, "order1"),
              statement(ORDER_IRI + 2, DCTERMS.IDENTIFIER, "order2"));

      SchemaMetadata schema =
          new SchemaMetadata(SCHEMA_NAME)
              .create(productTableWithSemantics("dcterms:title"), arrayOrderTable(true));

      try (SailRepositoryConnection connection = repository.getConnection()) {
        String query = GENERATOR.generate(schema.getTableMetadata("Order"));
        TupleQueryResult bindingSets = executeQuery(connection, query);
        assertHasResults(
            bindingSets,
            Map.of(
                SparqlVariableUtil.SUBJECT_NAME,
                ORDER_IRI,
                "products__name",
                "dog|cat",
                SparqlVariableUtil.SUBJECT_NAME + "products",
                PRODUCT_IRI + "|" + PRODUCT_IRI + 2,
                "id",
                "order1"));
      }
    }

    @Test
    void givenArrayReference_whenOptional_thenSurroundWitOptional() {
      SailRepository repository =
          repository(
              statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI)),
              statement(ORDER_IRI, DCTERMS.RELATION, Values.iri(PRODUCT_IRI + 2)),
              statement(PRODUCT_IRI + 2, DCTERMS.TITLE, "cat"),
              statement(PRODUCT_IRI, DCTERMS.TITLE, "dog"),
              statement(ORDER_IRI, DCTERMS.IDENTIFIER, "order1"),
              statement(ORDER_IRI + 2, DCTERMS.IDENTIFIER, "order2"));

      SchemaMetadata schema =
          new SchemaMetadata(SCHEMA_NAME)
              .create(productTableWithSemantics("dcterms:title"), arrayOrderTable(false));

      try (SailRepositoryConnection connection = repository.getConnection()) {
        String query = GENERATOR.generate(schema.getTableMetadata("Order"));
        TupleQueryResult bindingSets = executeQuery(connection, query);
        assertHasResults(
            bindingSets,
            Map.of(
                SparqlVariableUtil.SUBJECT_NAME,
                ORDER_IRI + 2,
                "id",
                "order2",
                "products__name",
                "",
                SparqlVariableUtil.SUBJECT_NAME + "products",
                ""),
            Map.of(
                SparqlVariableUtil.SUBJECT_NAME,
                ORDER_IRI,
                "id",
                "order1",
                "products__name",
                "dog|cat",
                SparqlVariableUtil.SUBJECT_NAME + "products",
                PRODUCT_IRI + "|" + PRODUCT_IRI + 2));
      }
    }
  }

  @Nested
  class SingleSemanticTest {

    @Test
    void shouldDoSimplifiedPatternOnSingleSemantic() {}

    @Test
    void whenRelationIsOptional_thenAddOptionalClause() {}
  }

  @Nested
  class MultipleSemanticsTest {

    @Test
    void shouldUseOrCoalesce() {}

    @Test
    void whenRequired_thenIncludeFilter() {}
  }

  @Nested
  class OntologyReferencesTest {

    @Test
    void shouldHandleOntologyReferences() {}

    @Test
    void shouldHandleOntologyArrayReferences() {}
  }

  private TableMetadata orderTable(boolean productRequired) {
    return TableMetadata.table(
        "Order",
        Column.column("id")
            .setPkey()
            .setType(ColumnType.STRING)
            .setSemantics("dcterms:identifier")
            .setRefTable("Order"),
        Column.column("product")
            .setType(ColumnType.REF)
            .setRefTable("Product")
            .setRequired(productRequired)
            .setSemantics("dcterms:relation"));
  }

  private TableMetadata arrayOrderTable(boolean productRequired) {
    return TableMetadata.table(
        "Order",
        Column.column("id")
            .setPkey()
            .setType(ColumnType.STRING)
            .setSemantics("dcterms:identifier")
            .setRefTable("Order"),
        Column.column("products")
            .setType(ColumnType.REF_ARRAY)
            .setRefTable("Product")
            .setRequired(productRequired)
            .setSemantics("dcterms:relation"));
  }

  private TableMetadata productTableWithSemantics(String... semantics) {
    return TableMetadata.table(
        "Product",
        Column.column("name").setType(ColumnType.STRING).setPkey().setSemantics(semantics));
  }
}

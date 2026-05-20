package org.molgenis.emx2.rdf.generators.query.generators;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.rdf.generators.MapperAssertions.*;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ReferenceColumnSparqlQueryGeneratorTest {

  private static final Variable ORDER_VAR = SparqlBuilder.var("order");

  private SchemaMetadata schema;
  private Database database;

  @BeforeEach
  void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(getClass().getSimpleName()).getMetadata();
  }

  @Test
  void givenReference_thenOnlyUseKey() {
    schema.create(
        productTableWithSemantics("schema:name")
            // Skip barcode because it is not a key
            .add(Column.column("barcode").setType(ColumnType.INT).setSemantics("schema:barcode")));

    TableMetadata order = schema.create(orderTable(true));
    Column column = order.getColumn("product");
    ReferenceColumnSparqlQueryGenerator tableReferenceQuery =
        new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

    assertHasPatterns(
        tableReferenceQuery,
        "?order schema:product ?product .",
        "?product schema:name ?product__name .");
    assertHasSelectors(tableReferenceQuery, "?product__name");
    assertHasGroupBy(tableReferenceQuery, "?product__name");
  }

  @Test
  void givenReference_whenPrimaryKeyHasNoSemantics_thenSkip() {
    schema.create(productTableWithSemantics());
    TableMetadata order = schema.create(orderTable(true));
    Column column = order.getColumn("product");
    ReferenceColumnSparqlQueryGenerator tableReferenceQuery =
        new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

    assertHasPatterns(tableReferenceQuery, "?order schema:product ?product .");
    assertTrue(tableReferenceQuery.getSelectors().isEmpty());
    assertTrue(tableReferenceQuery.getGroupBy().isEmpty());
  }

  @Test
  void givenReference_whenOptional_thenAddOptional() {
    schema.create(productTableWithSemantics("schema:name"));

    TableMetadata order = schema.create(orderTable(false));
    Column column = order.getColumn("product");
    ReferenceColumnSparqlQueryGenerator tableReferenceQuery =
        new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

    assertHasPatterns(
        tableReferenceQuery,
        """
        OPTIONAL { ?order schema:product ?product .
        ?product schema:name ?product__name . }""");
    assertHasSelectors(tableReferenceQuery, "?product__name");
    assertHasGroupBy(tableReferenceQuery, "?product__name");
  }

  @Test
  void givenReference_whenCompositeKey_thenUseAllKeys() {
    schema.create(
        productTableWithSemantics("schema:name")
            .add(
                Column.column("barcode")
                    .setType(ColumnType.INT)
                    .setPkey()
                    .setSemantics("schema:barcode")));

    TableMetadata order = schema.create(orderTable(true));
    Column column = order.getColumn("product");
    ReferenceColumnSparqlQueryGenerator mapper =
        new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

    assertHasPatterns(
        mapper,
        "?order schema:product ?product .",
        "?product schema:name ?product__name .",
        "?product schema:barcode ?product__barcode .");
    assertHasSelectors(mapper, "?product__name", "?product__barcode");
    assertHasGroupBy(mapper, "?product__name", "?product__barcode");
  }

  @Test
  void givenReference_whenCompositeKeyPartIsReference_thenUseAllSubkeys() {
    schema.create(
        TableMetadata.table(
            "Manufacturer",
            Column.column("name").setType(ColumnType.STRING).setPkey().setSemantics("schema:name"),
            Column.column("manufacturer_id")
                .setType(ColumnType.INT)
                .setPkey()
                .setSemantics("schema:id")));
    schema.create(
        productTableWithSemantics("schema:name")
            .add(
                Column.column("manufacturer")
                    .setType(ColumnType.REF)
                    .setPkey()
                    .setRefTable("Manufacturer")
                    .setSemantics("schema:manufacturer")));

    TableMetadata order = schema.create(orderTable(true));
    Column column = order.getColumn("product");
    ReferenceColumnSparqlQueryGenerator mapper =
        new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

    assertHasPatterns(
        mapper,
        "?order schema:product ?product .",
        "?product schema:name ?product__name .",
        "?product schema:manufacturer ?product__manufacturer .",
        "?product__manufacturer schema:name ?product__manufacturer__name .",
        "?product__manufacturer schema:id ?product__manufacturer__manufacturer_id .");
    assertHasSelectors(
        mapper,
        "?product__name",
        "?product__manufacturer__name",
        "?product__manufacturer__manufacturer_id");
    assertHasGroupBy(
        mapper,
        "?product__name",
        "?product__manufacturer__name",
        "?product__manufacturer__manufacturer_id");
  }

  @Nested
  class ReferenceArrayTest {

    @Test
    void givenArrayReference_thenUseCollectionMapper() {
      schema.create(productTableWithSemantics("schema:name"));
      TableMetadata order =
          schema.create(
              TableMetadata.table(
                  "Order",
                  Column.column("id")
                      .setPkey()
                      .setType(ColumnType.STRING)
                      .setSemantics("schema:id"),
                  Column.column("product")
                      .setType(ColumnType.REF_ARRAY)
                      .setRefTable("Product")
                      .setRequired(true)
                      .setSemantics("schema:product")));

      Column column = order.getColumn("product");
      ReferenceColumnSparqlQueryGenerator mapper =
          new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);
      assertHasPatterns(
          mapper,
          "?order schema:product ?product .",
          "?product schema:name ?product__name_single .");
      assertHasSelectors(
          mapper,
          "( GROUP_CONCAT( DISTINCT STR( ?product__name_single ) ; SEPARATOR = ',' ) AS ?product__name )");
      assertHasGroupBy(mapper);
    }

    @Test
    void givenArrayReference_whenOptional_thenSurroundWitOptional() {
      schema.create(productTableWithSemantics("schema:name"));
      TableMetadata order =
          schema.create(
              TableMetadata.table(
                  "Order",
                  Column.column("id")
                      .setPkey()
                      .setType(ColumnType.STRING)
                      .setSemantics("schema:id"),
                  Column.column("product")
                      .setType(ColumnType.REF_ARRAY)
                      .setRefTable("Product")
                      .setRequired(false)
                      .setSemantics("schema:product")));

      Column column = order.getColumn("product");
      ReferenceColumnSparqlQueryGenerator mapper =
          new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);
      assertHasPatterns(
          mapper,
          """
          OPTIONAL { ?order schema:product ?product .
          ?product schema:name ?product__name_single . }""");
      assertHasSelectors(
          mapper,
          "( GROUP_CONCAT( DISTINCT STR( ?product__name_single ) ; SEPARATOR = ',' ) AS ?product__name )");
      assertHasGroupBy(mapper);
    }
  }

  @Test
  void shouldResolveCrossSchemaReferences() {
    Schema productSchema = database.dropCreateSchema(getClass().getSimpleName() + "Products");
    productSchema.create(productTableWithSemantics("schema:name"));

    // To check whether we always check on schema's, not just when we don't find the table.
    schema.create(productTableWithSemantics("schema:invalid"));
    TableMetadata orders =
        schema.create(
            TableMetadata.table(
                "Order",
                Column.column("id").setPkey().setType(ColumnType.STRING).setSemantics("schema:id"),
                Column.column("product")
                    .setType(ColumnType.REF)
                    .setRefTable("Product")
                    .setRefSchemaName(productSchema.getName())
                    .setRequired(true)
                    .setSemantics("schema:product")));
    Column column = orders.getColumn("product");
    ReferenceColumnSparqlQueryGenerator mapper =
        new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

    assertHasPatterns(
        mapper, "?order schema:product ?product .", "?product schema:name ?product__name .");
    assertHasSelectors(mapper, "?product__name");
    assertHasGroupBy(mapper, "?product__name");
    database.dropSchemaIfExists(getClass().getSimpleName() + "Products");
    database.dropSchemaIfExists(getClass().getSimpleName());
  }

  @Nested
  class SingleSemanticTest {

    @Test
    void shouldDoSimplifiedPatternOnSingleSemantic() {
      schema.create(productTableWithSemantics("schema:name"));

      TableMetadata order = schema.create(orderTable(true));
      Column column = order.getColumn("product");
      ReferenceColumnSparqlQueryGenerator mapper =
          new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

      assertHasPatterns(
          mapper, "?order schema:product ?product .", "?product schema:name ?product__name .");
      assertHasSelectors(mapper, "?product__name");
      assertHasGroupBy(mapper, "?product__name");
    }

    @Test
    void whenRelationIsOptional_thenAddOptionalClause() {
      schema.create(productTableWithSemantics("schema:name"));

      TableMetadata order = schema.create(orderTable(false));
      Column column = order.getColumn("product");
      ReferenceColumnSparqlQueryGenerator mapper =
          new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

      assertHasPatterns(
          mapper,
          """
          OPTIONAL { ?order schema:product ?product .
          ?product schema:name ?product__name . }""");
      assertHasSelectors(mapper, "?product__name");
      assertHasGroupBy(mapper, "?product__name");
    }
  }

  @Nested
  class MultipleSemanticsTest {

    @Test
    void shouldUseOrCoalesce() {
      schema.create(
          productTableWithSemantics("schema:name", "schema:alternativeName", "schema:altName"));

      TableMetadata order = schema.create(orderTable(false));
      Column column = order.getColumn("product");
      ReferenceColumnSparqlQueryGenerator mapper =
          new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

      assertHasPatterns(
          mapper,
          """
        OPTIONAL { ?order schema:product ?product .
        OPTIONAL { OPTIONAL { ?product schema:name ?product__name0 . }
        OPTIONAL { ?product schema:alternativeName ?product__name1 . }
        OPTIONAL { ?product schema:altName ?product__name2 . }
        BIND( COALESCE( ?product__name0, ?product__name1, ?product__name2 ) AS ?product__name ) }
        FILTER ( BOUND( ?product__name ) ) }""");
      assertHasSelectors(mapper, "?product__name");
      assertHasGroupBy(mapper, "?product__name");
    }

    @Test
    void whenRequired_thenIncludeFilter() {
      schema.create(
          productTableWithSemantics("schema:name", "schema:alternativeName", "schema:altName"));

      TableMetadata order = schema.create(orderTable(true));
      Column column = order.getColumn("product");
      ReferenceColumnSparqlQueryGenerator mapper =
          new ReferenceColumnSparqlQueryGenerator(ORDER_VAR, column);

      assertHasPatterns(
          mapper,
          "?order schema:product ?product .",
          """
          OPTIONAL { OPTIONAL { ?product schema:name ?product__name0 . }
          OPTIONAL { ?product schema:alternativeName ?product__name1 . }
          OPTIONAL { ?product schema:altName ?product__name2 . }
          BIND( COALESCE( ?product__name0, ?product__name1, ?product__name2 ) AS ?product__name ) }""",
          "FILTER ( BOUND( ?product__name ) )");
      assertHasSelectors(mapper, "?product__name");
      assertHasGroupBy(mapper, "?product__name");
    }
  }

  @Nested
  class OntologyReferencesTest {

    private final Variable productVar = SparqlBuilder.var("product");

    @Test
    void shouldHandleOntologyReferences() {
      schema.create(TableMetadata.table("ProductType").setTableType(TableType.ONTOLOGIES));
      TableMetadata product =
          schema.create(
              productTableWithSemantics("schema:name")
                  .add(
                      Column.column("type")
                          .setType(ColumnType.ONTOLOGY)
                          .setSemantics("schema:type")
                          .setRefTable("ProductType")));

      Column column = product.getColumn("type");
      ReferenceColumnSparqlQueryGenerator mapper =
          new ReferenceColumnSparqlQueryGenerator(productVar, column);
      assertHasPatterns(mapper, "OPTIONAL { ?product schema:type ?type . }");
      assertHasSelectors(mapper, "?type");
      assertHasGroupBy(mapper, "?type");
    }

    @Test
    void shouldHandleOntologyArrayReferences() {
      schema.create(TableMetadata.table("ProductTag").setTableType(TableType.ONTOLOGIES));
      TableMetadata product =
          schema.create(
              productTableWithSemantics("schema:name")
                  .add(
                      Column.column("tag")
                          .setType(ColumnType.ONTOLOGY_ARRAY)
                          .setSemantics("schema:tag")
                          .setRefTable("ProductTag")));

      Column column = product.getColumn("tag");
      ReferenceColumnSparqlQueryGenerator mapper =
          new ReferenceColumnSparqlQueryGenerator(productVar, column);
      assertHasPatterns(mapper, "OPTIONAL { ?product schema:tag ?tag_single . }");
      assertHasSelectors(
          mapper, "( GROUP_CONCAT( DISTINCT STR( ?tag_single ) ; SEPARATOR = ',' ) AS ?tag )");
      assertHasGroupBy(mapper);
    }
  }

  private TableMetadata orderTable(boolean productRequired) {
    return TableMetadata.table(
        "Order",
        Column.column("id").setPkey().setType(ColumnType.STRING).setSemantics("schema:id"),
        Column.column("product")
            .setType(ColumnType.REF)
            .setRefTable("Product")
            .setRequired(productRequired)
            .setSemantics("schema:product"));
  }

  private TableMetadata productTableWithSemantics(String... semantics) {
    return TableMetadata.table(
        "Product",
        Column.column("name").setType(ColumnType.STRING).setPkey().setSemantics(semantics),
        Column.column("price").setType(ColumnType.DECIMAL));
  }
}

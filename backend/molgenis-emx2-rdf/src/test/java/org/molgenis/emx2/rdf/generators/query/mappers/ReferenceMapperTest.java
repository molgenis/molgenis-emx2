package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.molgenis.emx2.rdf.generators.query.mappers.MapperAssertions.assertHasSelectors;
import static org.molgenis.emx2.rdf.generators.query.mappers.MapperAssertions.assertPatternsMatch;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ReferenceMapperTest {

  private static final Variable ORDER_VAR = SparqlBuilder.var("order");

  private SchemaMetadata schema;

  @BeforeEach
  void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(getClass().getSimpleName()).getMetadata();
  }

  @Test
  void givenReference_thenOnlyUseKey() {
    schema.create(
        productTableWithSemantics("product:name")
            // Skip barcode because it is not a key
            .add(Column.column("barcode").setType(ColumnType.INT).setSemantics("product:barcode")));

    TableMetadata order = schema.create(orderTable(true));
    Column column = order.getColumn("product");
    ReferenceMapper tableReferenceQuery = new ReferenceMapper(ORDER_VAR, column);

    assertPatternsMatch(
        tableReferenceQuery,
        "?order orders:product ?product .",
        "?product product:name ?product_name .");
    assertHasSelectors(tableReferenceQuery, "?product_name");
  }

  @Test
  void givenReference_whenOptional_thenAddOptional() {
    schema.create(productTableWithSemantics("product:name"));

    TableMetadata order = schema.create(orderTable(false));
    Column column = order.getColumn("product");
    ReferenceMapper tableReferenceQuery = new ReferenceMapper(ORDER_VAR, column);

    assertPatternsMatch(
        tableReferenceQuery,
        """
        OPTIONAL { ?order orders:product ?product .
        ?product product:name ?product_name . }""");
    assertHasSelectors(tableReferenceQuery, "?product_name");
  }

  @Test
  void givenReference_whenCompositeKey_thenUseAllKeys() {
    schema.create(
        productTableWithSemantics("product:name")
            .add(
                Column.column("barcode")
                    .setType(ColumnType.INT)
                    .setPkey()
                    .setSemantics("product:barcode")));

    TableMetadata order = schema.create(orderTable(true));
    Column column = order.getColumn("product");
    ReferenceMapper mapper = new ReferenceMapper(ORDER_VAR, column);

    assertPatternsMatch(
        mapper,
        "?order orders:product ?product .",
        "?product product:name ?product_name .",
        "?product product:barcode ?product_barcode .");
    assertHasSelectors(mapper, "?product_name", "?product_barcode");
  }

  @Test
  void givenReference_whenCompositeKeyPartIsReference_thenUseAllSubkeys() {
    schema.create(
        TableMetadata.table(
            "Manufacturer",
            Column.column("name")
                .setType(ColumnType.STRING)
                .setPkey()
                .setSemantics("manufacturer:name"),
            Column.column("id").setType(ColumnType.INT).setPkey().setSemantics("manufacturer:id")));
    schema.create(
        productTableWithSemantics("product:name")
            .add(
                Column.column("manufacturer")
                    .setType(ColumnType.REF)
                    .setPkey()
                    .setRefTable("Manufacturer")
                    .setSemantics("product:manufacturer")));

    TableMetadata order = schema.create(orderTable(true));
    Column column = order.getColumn("product");
    ReferenceMapper mapper = new ReferenceMapper(ORDER_VAR, column);

    assertPatternsMatch(
        mapper,
        "?order orders:product ?product .",
        "?product product:name ?product_name .",
        "?product product:manufacturer ?product_manufacturer .",
        "?product_manufacturer manufacturer:name ?product_manufacturer_name .",
        "?product_manufacturer manufacturer:id ?product_manufacturer_id .");
    assertHasSelectors(
        mapper, "?product_name", "?product_manufacturer_name", "?product_manufacturer_id");
  }

  @Nested
  class ReferenceArrayTest {

    @Test
    void givenArrayReference_thenUseCollectionMapper() {
      schema.create(productTableWithSemantics("product:name"));
      TableMetadata order =
          schema.create(
              TableMetadata.table(
                  "Order",
                  Column.column("id")
                      .setPkey()
                      .setType(ColumnType.STRING)
                      .setSemantics("orders:id"),
                  Column.column("product")
                      .setType(ColumnType.REF_ARRAY)
                      .setRefTable("Product")
                      .setRequired(true)
                      .setSemantics("orders:product")));

      Column column = order.getColumn("product");
      ReferenceMapper mapper = new ReferenceMapper(ORDER_VAR, column);
      assertPatternsMatch(
          mapper,
          "?order orders:product ?product .",
          "?product product:name ?product_name_single .");
      assertHasSelectors(
          mapper, "( GROUP_CONCAT( ?product_name_single ; SEPARATOR = , ) AS ?product_name )");
    }

    @Test
    void givenArrayReference_whenOptional_thenSurroundWitOptional() {
      schema.create(productTableWithSemantics("product:name"));
      TableMetadata order =
          schema.create(
              TableMetadata.table(
                  "Order",
                  Column.column("id")
                      .setPkey()
                      .setType(ColumnType.STRING)
                      .setSemantics("orders:id"),
                  Column.column("product")
                      .setType(ColumnType.REF_ARRAY)
                      .setRefTable("Product")
                      .setRequired(false)
                      .setSemantics("orders:product")));

      Column column = order.getColumn("product");
      ReferenceMapper mapper = new ReferenceMapper(ORDER_VAR, column);
      assertPatternsMatch(
          mapper,
          """
          OPTIONAL { ?order orders:product ?product .
          ?product product:name ?product_name_single . }""");
      assertHasSelectors(
          mapper, "( GROUP_CONCAT( ?product_name_single ; SEPARATOR = , ) AS ?product_name )");
    }
  }

  @Nested
  class SingleSemanticTest {

    @Test
    void shouldDoSimplifiedPatternOnSingleSemantic() {
      schema.create(productTableWithSemantics("product:name"));

      TableMetadata order = schema.create(orderTable(true));
      Column column = order.getColumn("product");
      ReferenceMapper mapper = new ReferenceMapper(ORDER_VAR, column);

      assertPatternsMatch(
          mapper, "?order orders:product ?product .", "?product product:name ?product_name .");
      assertHasSelectors(mapper, "?product_name");
    }

    @Test
    void whenRelationIsOptional_thenAddOptionalClause() {
      schema.create(productTableWithSemantics("product:name"));

      TableMetadata order = schema.create(orderTable(false));
      Column column = order.getColumn("product");
      ReferenceMapper mapper = new ReferenceMapper(ORDER_VAR, column);

      assertPatternsMatch(
          mapper,
          """
          OPTIONAL { ?order orders:product ?product .
          ?product product:name ?product_name . }""");
      assertHasSelectors(mapper, "?product_name");
    }
  }

  @Nested
  class MultipleSemanticsTest {

    @Test
    void shouldUseOrCoalesce() {
      schema.create(
          productTableWithSemantics("product:name", "product:alternativeName", "product:altName"));

      TableMetadata order = schema.create(orderTable(false));
      Column column = order.getColumn("product");
      ReferenceMapper mapper = new ReferenceMapper(ORDER_VAR, column);

      assertPatternsMatch(
          mapper,
          """
        OPTIONAL { ?order orders:product ?product .
        OPTIONAL { OPTIONAL { ?product product:name ?product_name0 . }
        OPTIONAL { ?product product:alternativeName ?product_name1 . }
        OPTIONAL { ?product product:altName ?product_name2 . }
        BIND( COALESCE( ?product_name0, ?product_name1, ?product_name2 ) AS ?product_name ) }
        FILTER ( BOUND( ?product_name ) ) }""");
      assertHasSelectors(mapper, "?product_name");
    }

    @Test
    void whenRequired_thenIncludeFilter() {
      schema.create(
          productTableWithSemantics("product:name", "product:alternativeName", "product:altName"));

      TableMetadata order = schema.create(orderTable(true));
      Column column = order.getColumn("product");
      ReferenceMapper mapper = new ReferenceMapper(ORDER_VAR, column);

      assertPatternsMatch(
          mapper,
          "?order orders:product ?product .",
          """
          OPTIONAL { OPTIONAL { ?product product:name ?product_name0 . }
          OPTIONAL { ?product product:alternativeName ?product_name1 . }
          OPTIONAL { ?product product:altName ?product_name2 . }
          BIND( COALESCE( ?product_name0, ?product_name1, ?product_name2 ) AS ?product_name ) }""",
          "FILTER ( BOUND( ?product_name ) )");
      assertHasSelectors(mapper, "?product_name");
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
              productTableWithSemantics("product:name")
                  .add(
                      Column.column("type")
                          .setType(ColumnType.ONTOLOGY)
                          .setSemantics("product:type")
                          .setRefTable("ProductType")));

      Column column = product.getColumn("type");
      ReferenceMapper mapper = new ReferenceMapper(productVar, column);
      assertPatternsMatch(mapper, "OPTIONAL { ?product product:type ?type . }");
      assertHasSelectors(mapper, "?type");
    }

    @Test
    void shouldHandleOntologyArrayReferences() {
      schema.create(TableMetadata.table("ProductTag").setTableType(TableType.ONTOLOGIES));
      TableMetadata product =
          productTableWithSemantics("product:name")
              .add(
                  Column.column("tag")
                      .setType(ColumnType.ONTOLOGY_ARRAY)
                      .setSemantics("product:tag")
                      .setRefTable("ProductTag"));

      Column column = product.getColumn("tag");
      ReferenceMapper mapper = new ReferenceMapper(productVar, column);
      assertPatternsMatch(mapper, "OPTIONAL { ?product product:tag ?tag_single . }");
      assertHasSelectors(mapper, "( GROUP_CONCAT( ?tag_single ; SEPARATOR = , ) AS ?tag )");
    }
  }

  private TableMetadata orderTable(boolean productRequired) {
    return TableMetadata.table(
        "Order",
        Column.column("id").setPkey().setType(ColumnType.STRING).setSemantics("orders:id"),
        Column.column("product")
            .setType(ColumnType.REF)
            .setRefTable("Product")
            .setRequired(productRequired)
            .setSemantics("orders:product"));
  }

  private TableMetadata productTableWithSemantics(String... semantics) {
    return TableMetadata.table(
        "Product",
        Column.column("name").setType(ColumnType.STRING).setPkey().setSemantics(semantics),
        Column.column("price").setType(ColumnType.DECIMAL));
  }
}

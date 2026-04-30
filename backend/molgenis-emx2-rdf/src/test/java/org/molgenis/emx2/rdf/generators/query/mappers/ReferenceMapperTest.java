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

  private Schema schema;

  @BeforeEach
  void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(getClass().getSimpleName());
  }

  @Test
  void givenReference_thenOnlyUseKey() {
    schema.create(
        productTableWithSemantics("product:name")
            // Skip barcode because it is not a key
            .add(Column.column("barcode").setType(ColumnType.INT).setSemantics("product:barcode")));
    TableMetadata order = schema.create(orderTable(true)).getMetadata();

    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();

    ReferenceMapper tableReferenceQuery =
        new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());
    assertPatternsMatch(tableReferenceQuery, "?product product:name ?product_name .");
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

    TableMetadata order = schema.create(orderTable(true)).getMetadata();
    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();
    ReferenceMapper tableReferenceQuery =
        new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());
    assertPatternsMatch(
        tableReferenceQuery,
        "?product product:name ?product_name .",
        "?product product:barcode ?product_barcode .");
    assertHasSelectors(tableReferenceQuery, "?product_name", "?product_barcode");
  }

  @Test
  void givenReference_whenKeyHasMultipleSemantics_thenFirstMatch() {
    schema.create(
        productTableWithSemantics("product:name", "product:alt_name", "product:alt_alt_name"));

    TableMetadata order = schema.create(orderTable(true)).getMetadata();
    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();

    ReferenceMapper tableReferenceQuery =
        new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());
    assertPatternsMatch(
        tableReferenceQuery,
        """
        OPTIONAL { OPTIONAL { ?product product:name ?product_name0 . }
        OPTIONAL { ?product product:alt_name ?product_name1 . }
        OPTIONAL { ?product product:alt_alt_name ?product_name2 . }
        BIND( COALESCE( ?product_name0, ?product_name1, ?product_name2 ) AS ?product_name ) }""",
        "FILTER ( BOUND( ?product_name ) )");
    assertHasSelectors(tableReferenceQuery, "?product_name");
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
    TableMetadata order = schema.create(orderTable(true)).getMetadata();

    Variable startingPoint = SparqlBuilder.var("product");
    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();
    ReferenceMapper tableReferenceQuery =
        new ReferenceMapper(startingPoint, reference, schema.getMetadata());
    assertPatternsMatch(
        tableReferenceQuery,
        "?product product:name ?product_name .",
        "?product product:manufacturer ?product_manufacturer .",
        "?product_manufacturer manufacturer:name ?product_manufacturer_name .",
        "?product_manufacturer manufacturer:id ?product_manufacturer_id .");
    assertHasSelectors(
        tableReferenceQuery,
        "?product_name",
        "?product_manufacturer_name",
        "?product_manufacturer_id");
  }

  @Nested
  class SingleSemanticTest {

    @Test
    void shouldDoSimplifiedPatternOnSingleSemantic() {
      schema.create(productTableWithSemantics("product:name"));
      TableMetadata order = schema.create(orderTable(true)).getMetadata();

      Variable startingPoint = SparqlBuilder.var("product");
      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferenceMapper tableReferenceQuery =
          new ReferenceMapper(startingPoint, reference, schema.getMetadata());
      assertPatternsMatch(tableReferenceQuery, "?product product:name ?product_name .");
      assertHasSelectors(tableReferenceQuery, "?product_name");
    }

    @Test
    void whenRelationIsOptional_thenAddOptionalClause() {
      schema.create(productTableWithSemantics("product:name"));

      TableMetadata order = schema.create(orderTable(false)).getMetadata();
      Variable startingPoint = SparqlBuilder.var("product");
      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferenceMapper tableReferenceQuery =
          new ReferenceMapper(startingPoint, reference, schema.getMetadata());
      assertPatternsMatch(
          tableReferenceQuery, "OPTIONAL { ?product product:name ?product_name . }");
      assertHasSelectors(tableReferenceQuery, "?product_name");
    }
  }

  @Nested
  class MultipleSemanticsTest {

    @Test
    void shouldUseOrClause() {
      schema.create(
          productTableWithSemantics("product:name", "product:alternativeName", "product:altName"));
      TableMetadata order = schema.create(orderTable(false)).getMetadata();

      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferenceMapper tableReferenceQuery =
          new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());

      assertPatternsMatch(
          tableReferenceQuery,
          """
          OPTIONAL { OPTIONAL { ?product product:name ?product_name0 . }
          OPTIONAL { ?product product:alternativeName ?product_name1 . }
          OPTIONAL { ?product product:altName ?product_name2 . }
          BIND( COALESCE( ?product_name0, ?product_name1, ?product_name2 ) AS ?product_name ) }""");
      assertHasSelectors(tableReferenceQuery, "?product_name");
    }

    @Test
    void whenRequired_thenIncludeFilter() {
      schema.create(
          productTableWithSemantics("product:name", "product:alternativeName", "product:altName"));
      TableMetadata order = schema.create(orderTable(true)).getMetadata();

      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferenceMapper tableReferenceQuery =
          new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());

      assertPatternsMatch(
          tableReferenceQuery,
          """
          OPTIONAL { OPTIONAL { ?product product:name ?product_name0 . }
          OPTIONAL { ?product product:alternativeName ?product_name1 . }
          OPTIONAL { ?product product:altName ?product_name2 . }
          BIND( COALESCE( ?product_name0, ?product_name1, ?product_name2 ) AS ?product_name ) }""",
          "FILTER ( BOUND( ?product_name ) )");
      assertHasSelectors(tableReferenceQuery, "?product_name");
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

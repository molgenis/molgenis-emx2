package org.molgenis.emx2.harvester;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ReferencePatternsTest {

  private Database database;
  private Schema schema;

  @BeforeEach
  void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void givenReference_thenOnlyUseKey() {
    schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
    schema.create(
        addProductTableWithSemantics("product:name")
            // Skip barcode because it is not a key
            .add(Column.column("barcode").setType(ColumnType.INT).setSemantics("product:barcode")));
    TableMetadata order = schema.create(addOrderTable(true)).getMetadata();

    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();

    ReferencePatterns referencePatterns =
        new ReferencePatterns(SparqlBuilder.var("product"), reference, schema.getMetadata());
    assertPatternsMatch(referencePatterns, "?product product:name ?Product_name .");
  }

  @Test
  void givenReference_whenCompositeKey_thenUseAllKeys() {
    schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
    schema.create(
        addProductTableWithSemantics("product:name")
            .add(
                Column.column("barcode")
                    .setType(ColumnType.INT)
                    .setPkey()
                    .setSemantics("product:barcode")));

    TableMetadata order = schema.create(addOrderTable(true)).getMetadata();
    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();
    ReferencePatterns referencePatterns =
        new ReferencePatterns(SparqlBuilder.var("product"), reference, schema.getMetadata());
    assertPatternsMatch(
        referencePatterns,
        "?product product:name ?Product_name .",
        "?product product:barcode ?Product_barcode .");
  }

  @Nested
  class SingleSemanticTest {

    @Test
    void shouldDoSimplifiedPatternOnSingleSemantic() {
      schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
      schema.create(addProductTableWithSemantics("product:name"));
      TableMetadata order = schema.create(addOrderTable(true)).getMetadata();

      Variable startingPoint = SparqlBuilder.var("product");
      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferencePatterns referencePatterns =
          new ReferencePatterns(startingPoint, reference, schema.getMetadata());
      assertPatternsMatch(referencePatterns, "?product product:name ?Product_name .");
    }

    @Test
    void whenRelationIsOptional_thenAddOptionalClause() {
      schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
      schema.create(addProductTableWithSemantics("product:name"));

      TableMetadata order = schema.create(addOrderTable(false)).getMetadata();
      Variable startingPoint = SparqlBuilder.var("product");
      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferencePatterns referencePatterns =
          new ReferencePatterns(startingPoint, reference, schema.getMetadata());
      assertPatternsMatch(referencePatterns, "OPTIONAL { ?product product:name ?Product_name . }");
    }
  }

  @Nested
  class MultipleSemanticsTest {

    @Test
    void shouldUseOrClause() {
      schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
      schema.create(
          addProductTableWithSemantics(
              "product:name", "product:alternativeName", "product:altName"));
      TableMetadata order = schema.create(addOrderTable(false)).getMetadata();

      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferencePatterns referencePatterns =
          new ReferencePatterns(SparqlBuilder.var("product"), reference, schema.getMetadata());

      assertPatternsMatch(
          referencePatterns,
          "OPTIONAL { ?product product:name ?Product_name0 . }",
          "OPTIONAL { ?product product:alternativeName ?Product_name1 . }",
          "OPTIONAL { ?product product:altName ?Product_name2 . }",
          "BIND( COALESCE( ?Product_name0, ?Product_name1, ?Product_name2 ) AS ?Product_name )");
    }

    @Test
    void whenRequired_thenIncludeFilter() {
      schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
      schema.create(
          addProductTableWithSemantics(
              "product:name", "product:alternativeName", "product:altName"));
      TableMetadata order = schema.create(addOrderTable(true)).getMetadata();

      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferencePatterns referencePatterns =
          new ReferencePatterns(SparqlBuilder.var("product"), reference, schema.getMetadata());

      assertPatternsMatch(
          referencePatterns,
          "OPTIONAL { ?product product:name ?Product_name0 . }",
          "OPTIONAL { ?product product:alternativeName ?Product_name1 . }",
          "OPTIONAL { ?product product:altName ?Product_name2 . }",
          "BIND( COALESCE( ?Product_name0, ?Product_name1, ?Product_name2 ) AS ?Product_name )",
          "FILTER ( BOUND( ?Product_name ) )");
    }
  }

  private void assertPatternsMatch(ReferencePatterns reference, String... expectedPatterns) {
    List<GraphPattern> patterns = reference.getPattern();
    assertEquals(expectedPatterns.length, patterns.size());
    for (int i = 0; i < expectedPatterns.length; i++) {
      assertEquals(expectedPatterns[i], patterns.get(i).getQueryString());
    }
  }

  private TableMetadata addOrderTable(boolean productRequired) {
    return TableMetadata.table(
        "Order",
        Column.column("id").setPkey().setType(ColumnType.STRING).setSemantics("orders:id"),
        Column.column("product")
            .setType(ColumnType.REF)
            .setRefTable("Product")
            .setRequired(productRequired)
            .setSemantics("orders:product"));
  }

  private TableMetadata addProductTableWithSemantics(String... semantics) {
    return TableMetadata.table(
        "Product",
        Column.column("name").setType(ColumnType.STRING).setPkey().setSemantics(semantics),
        Column.column("price").setType(ColumnType.DECIMAL));
  }
}

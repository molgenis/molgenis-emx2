package org.molgenis.emx2.rdf.generators.query.mappers;

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

class ReferenceMapperTest {

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

    ReferenceMapper tableReferenceQuery =
        new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());
    assertPatternsMatch(tableReferenceQuery, "?product product:name ?Product_name .");
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
    ReferenceMapper tableReferenceQuery =
        new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());
    assertPatternsMatch(
        tableReferenceQuery,
        "?product product:name ?Product_name .",
        "?product product:barcode ?Product_barcode .");
  }

  @Test
  void givenReference_whenKeyHasMultipleSemantics_thenFirstMatch() {
    schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
    schema.create(
        addProductTableWithSemantics("product:name", "product:alt_name", "product:alt_alt_name"));

    TableMetadata order = schema.create(addOrderTable(true)).getMetadata();
    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();

    ReferenceMapper tableReferenceQuery =
        new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());
    assertPatternsMatch(
        tableReferenceQuery,
        """
        OPTIONAL { OPTIONAL { ?product product:name ?Product_name0 . }
        OPTIONAL { ?product product:alt_name ?Product_name1 . }
        OPTIONAL { ?product product:alt_alt_name ?Product_name2 . }
        BIND( COALESCE( ?Product_name0, ?Product_name1, ?Product_name2 ) AS ?Product_name ) }""",
        "FILTER ( BOUND( ?Product_name ) )");
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
      ReferenceMapper tableReferenceQuery =
          new ReferenceMapper(startingPoint, reference, schema.getMetadata());
      assertPatternsMatch(tableReferenceQuery, "?product product:name ?Product_name .");
    }

    @Test
    void whenRelationIsOptional_thenAddOptionalClause() {
      schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
      schema.create(addProductTableWithSemantics("product:name"));

      TableMetadata order = schema.create(addOrderTable(false)).getMetadata();
      Variable startingPoint = SparqlBuilder.var("product");
      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferenceMapper tableReferenceQuery =
          new ReferenceMapper(startingPoint, reference, schema.getMetadata());
      assertPatternsMatch(
          tableReferenceQuery, "OPTIONAL { ?product product:name ?Product_name . }");
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
      ReferenceMapper tableReferenceQuery =
          new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());

      assertPatternsMatch(
          tableReferenceQuery,
          """
          OPTIONAL { OPTIONAL { ?product product:name ?Product_name0 . }
          OPTIONAL { ?product product:alternativeName ?Product_name1 . }
          OPTIONAL { ?product product:altName ?Product_name2 . }
          BIND( COALESCE( ?Product_name0, ?Product_name1, ?Product_name2 ) AS ?Product_name ) }""");
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
      ReferenceMapper tableReferenceQuery =
          new ReferenceMapper(SparqlBuilder.var("product"), reference, schema.getMetadata());

      assertPatternsMatch(
          tableReferenceQuery,
          """
          OPTIONAL { OPTIONAL { ?product product:name ?Product_name0 . }
          OPTIONAL { ?product product:alternativeName ?Product_name1 . }
          OPTIONAL { ?product product:altName ?Product_name2 . }
          BIND( COALESCE( ?Product_name0, ?Product_name1, ?Product_name2 ) AS ?Product_name ) }""",
          "FILTER ( BOUND( ?Product_name ) )");
    }
  }

  private void assertPatternsMatch(ReferenceMapper reference, String... expectedPatterns) {
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

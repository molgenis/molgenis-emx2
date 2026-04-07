package org.molgenis.emx2.harvester;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ReferenceQueryPartsTest {

  private Database database;
  private Schema schema;

  @BeforeEach
  void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void givenReference_thenOnlyUseKey() {
    schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
    schema
        .create(
            TableMetadata.table(
                "product",
                Column.column("id").setType(ColumnType.STRING).setPkey().setSemantics("product:id"),
                Column.column("name").setType(ColumnType.STRING).setSemantics("product:name"),
                Column.column("price").setType(ColumnType.DECIMAL)))
        .getMetadata();

    TableMetadata order = addOrderTable();
    Variable startingPoint = SparqlBuilder.var("product");
    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();
    ReferenceQueryParts referenceQueryParts =
        new ReferenceQueryParts(startingPoint, reference, schema.getMetadata());

    assertEquals(
        "{ ?product product:id product_id . }", referenceQueryParts.getPattern().getQueryString());
  }

  @Test
  void givenReference_whenCompositeKey_thenUseAllKeys() {
    schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
    schema
        .create(
            TableMetadata.table(
                "product",
                Column.column("id").setType(ColumnType.STRING).setPkey().setSemantics("product:id"),
                Column.column("barcode")
                    .setType(ColumnType.INT)
                    .setPkey()
                    .setSemantics("product:barcode")))
        .getMetadata();

    TableMetadata order = addOrderTable();
    Variable startingPoint = SparqlBuilder.var("product");
    Column column = order.getColumn("product");
    Reference reference = column.getReferences().getFirst();
    ReferenceQueryParts referenceQueryParts =
        new ReferenceQueryParts(startingPoint, reference, schema.getMetadata());

    assertEquals(
        """
        { ?product product:id product_id .
        ?product product:barcode product_barcode . }""",
        referenceQueryParts.getPattern().getQueryString());
  }

  @Nested
  class MultipleSemanticsTest {

    @Test
    void shouldUseOrClause() {
      schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
      schema
          .create(
              TableMetadata.table(
                  "product",
                  Column.column("name")
                      .setType(ColumnType.STRING)
                      .setPkey()
                      .setSemantics("product:name", "product:alternativeName", "product:altName"),
                  Column.column("price").setType(ColumnType.DECIMAL)))
          .getMetadata();

      TableMetadata order = addOrderTable(false);
      Variable startingPoint = SparqlBuilder.var("product");
      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferenceQueryParts referenceQueryParts =
          new ReferenceQueryParts(startingPoint, reference, schema.getMetadata());

      String queryString = referenceQueryParts.getPattern().getQueryString();
      assertEquals(
          """
              { OPTIONAL { ?product product:name ?product_name0 . }
              OPTIONAL { ?product product:alternativeName ?product_name1 . }
              OPTIONAL { ?product product:altName ?product_name2 . }
              BIND( COALESCE( ?product_name0, ?product_name1, ?product_name2 ) AS ?product_name ) }""",
          queryString);
    }

    @Test
    void whenRequired_thenIncludeFilter() {
      schema = database.dropCreateSchema(getClass().getSimpleName() + "_compositePkey");
      schema
          .create(
              TableMetadata.table(
                  "product",
                  Column.column("name")
                      .setType(ColumnType.STRING)
                      .setPkey()
                      .setSemantics("product:name", "product:alternativeName", "product:altName"),
                  Column.column("price").setType(ColumnType.DECIMAL)))
          .getMetadata();

      TableMetadata order = addOrderTable(true);
      Variable startingPoint = SparqlBuilder.var("product");
      Column column = order.getColumn("product");
      Reference reference = column.getReferences().getFirst();
      ReferenceQueryParts referenceQueryParts =
          new ReferenceQueryParts(startingPoint, reference, schema.getMetadata());

      String queryString = referenceQueryParts.getPattern().getQueryString();
      assertEquals(
          """
              { OPTIONAL { ?product product:name ?product_name0 . }
              OPTIONAL { ?product product:alternativeName ?product_name1 . }
              OPTIONAL { ?product product:altName ?product_name2 . }
              BIND( COALESCE( ?product_name0, ?product_name1, ?product_name2 ) AS ?product_name )
              FILTER ( BOUND( ?product_name ) ) }""",
          queryString);
    }
  }

  private TableMetadata addOrderTable() {
    return addOrderTable(false);
  }

  private TableMetadata addOrderTable(boolean required) {
    return schema
        .create(
            TableMetadata.table(
                "Order",
                Column.column("id").setPkey().setType(ColumnType.STRING).setSemantics("orders:id"),
                Column.column("product")
                    .setType(ColumnType.REF)
                    .setRefTable("product")
                    .setRequired(required)
                    .setSemantics("orders:product")))
        .getMetadata();
  }
}

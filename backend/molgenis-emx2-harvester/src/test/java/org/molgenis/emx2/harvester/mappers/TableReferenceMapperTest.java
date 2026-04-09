package org.molgenis.emx2.harvester.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TableReferenceMapperTest {

  @Test
  void givenReference_thenOnlyUseKey() {
    SchemaMetadata schema = new SchemaMetadata("test");
    schema.create(
        new TableMetadata("product")
            .add(
                Column.column("id").setSemantics("product:id").setPkey().setType(ColumnType.INT),
                Column.column("name").setSemantics("product:name").setType(ColumnType.STRING)));

    TableMetadata order =
        schema.create(
            new TableMetadata("order")
                .add(
                    Column.column("id").setSemantics("order:id").setPkey().setType(ColumnType.INT),
                    Column.column("product")
                        .setSemantics("order:product")
                        .setRefTable("product")
                        .setType(ColumnType.REF)));

    Reference reference = order.getColumn("product").getReferences().getFirst();
    ColumnMapper mapper =
        new TableReferenceMapper(
            schema, SparqlBuilder.var("foo"), order.getColumn("product"), reference);
    assertPatternsMatch(
        mapper, "?foo order:product ?product .", "?product product:id ?product.id .");
  }

  @Test
  void whenRefArray_thenUseFlatteningSelector() {
    SchemaMetadata schema = new SchemaMetadata("test");
    schema.create(
        new TableMetadata("product")
            .add(
                Column.column("id").setSemantics("product:id").setPkey().setType(ColumnType.INT),
                Column.column("name").setSemantics("product:name").setType(ColumnType.STRING)));

    TableMetadata order =
        schema.create(
            new TableMetadata("order")
                .add(
                    Column.column("id").setSemantics("order:id").setPkey().setType(ColumnType.INT),
                    Column.column("product")
                        .setSemantics("order:product")
                        .setRefTable("product")
                        .setType(ColumnType.REF_ARRAY)));

    Reference reference = order.getColumn("product").getReferences().getFirst();
    ColumnMapper mapper =
        new TableReferenceMapper(
            schema, SparqlBuilder.var("foo"), order.getColumn("product"), reference);
    assertPatternsMatch(
        mapper, "?foo order:product ?product .", "?product product:id ?product.id .");
  }

  @Test
  void givenReference_whenCompositeKey_thenHandleAllKeys() {
    SchemaMetadata schema = new SchemaMetadata("test");
    schema.create(
        new TableMetadata("product")
            .add(
                Column.column("id").setSemantics("product:id").setPkey().setType(ColumnType.INT),
                Column.column("name")
                    .setSemantics("product:name")
                    .setPkey()
                    .setType(ColumnType.STRING)));

    TableMetadata order =
        schema.create(
            new TableMetadata("order")
                .add(
                    Column.column("id").setSemantics("order:id").setPkey().setType(ColumnType.INT),
                    Column.column("product")
                        .setSemantics("order:product")
                        .setRefTable("product")
                        .setType(ColumnType.REF)));

    Column fromColumn = order.getColumn("product");
    Reference reference = fromColumn.getReferences().getFirst();
    ColumnMapper mapper =
        new TableReferenceMapper(schema, SparqlBuilder.var("foo"), fromColumn, reference);
    assertPatternsMatch(
        mapper,
        "?foo order:product ?product .",
        "?product product:id ?product.id .",
        "?product product:name ?product.name .");
  }

  /**
   * A -> B -> C PK B is composite of: - B.name - C.name
   *
   * <p>Reference is from A to B
   */
  @Test
  void givenReference_whenMultilayered_thenResolveAllLayers() {
    // todo
  }

  private void assertPatternsMatch(ColumnMapper mapper, String... expectedPatterns) {
    List<GraphPattern> patterns = mapper.getPattern();
    assertEquals(expectedPatterns.length, patterns.size());
    for (int i = 0; i < expectedPatterns.length; i++) {
      assertEquals(expectedPatterns[i], patterns.get(i).getQueryString());
    }
  }
}

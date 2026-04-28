package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class ColumnVariableNameTest {

  private TableMetadata order;
  private SchemaMetadata metadata;

  @BeforeEach
  void setup() {
    metadata =
        new SchemaMetadata("shop")
            .create(
                TableMetadata.table(
                    "product", Column.column("product_id").setType(ColumnType.STRING).setPkey()),
                TableMetadata.table(
                    "order",
                    Column.column("order_id").setType(ColumnType.STRING).setPkey(),
                    Column.column("product_id").setType(ColumnType.REF).setRefTable("product"),
                    Column.column("delivery method").setType(ColumnType.STRING)));
    order = metadata.getTableMetadata("order");
  }

  @Test
  void shouldGetColumnName() {
    Column column = order.getColumn("order_id");
    assertNameMatches(column, "order_id");
  }

  @Test
  void shouldReplaceSpace() {
    Column column = order.getColumn("delivery method");
    assertNameMatches(column, "delivery___method");
  }

  private static void assertNameMatches(Column column, String expected) {
    ColumnVariableName name = new ColumnVariableName(column);
    assertEquals(name.getSparqlName(), expected);
  }
}

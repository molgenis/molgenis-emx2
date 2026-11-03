package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlTableFragmentGenerator {

  private static final String SCHEMA = TestGraphqlTableFragmentGenerator.class.getSimpleName();
  private static final String NAME = "name";
  private static final String NEXT = "next";
  private static Schema schema;

  @BeforeAll
  static void setup() {
    schema = TestDatabaseFactory.getTestDatabase().dropCreateSchema(SCHEMA);
    schema.create(
        table("Leaf", column("id").setPkey(), column("leafLabel")),
        table(
            "Pair",
            column("leaf").setType(ColumnType.REF).setRefTable("Leaf").setPkey(),
            column("day").setPkey(),
            column("pairNote")),
        table(
            "Holder",
            column(NAME).setPkey(),
            column("pair").setType(ColumnType.REF).setRefTable("Pair")));
    schema.create(table("E", column(NAME).setPkey()));
    schema.create(
        table("D", column(NAME).setPkey(), column(NEXT).setType(ColumnType.REF).setRefTable("E")));
    schema.create(
        table("C", column(NAME).setPkey(), column(NEXT).setType(ColumnType.REF).setRefTable("D")));
    schema.create(
        table("B", column(NAME).setPkey(), column(NEXT).setType(ColumnType.REF).setRefTable("C")));
    schema.create(
        table("A", column(NAME).setPkey(), column(NEXT).setType(ColumnType.REF).setRefTable("B")));
  }

  @Test
  void aReferenceToACompositeKeyWhoseKeyPartIsAReferenceSelectsKeyFieldsOnly() {
    String selection =
        GraphqlTableFragmentGenerator.rowSelection(schema, schema.getTable("Holder").getMetadata());
    assertEquals("{name pair{leaf{id}day}mg_draft}", selection);
    assertFalse(selection.contains("leafLabel"));
    assertFalse(selection.contains("pairNote"));
  }

  @Test
  void generatedFragmentsExpandAReferenceChainNoDeeperThanTheirDepth() {
    Map<String, String> fragments = GraphqlTableFragmentGenerator.generate(schema);
    assertEquals(1, count(fragments.get("AAllFields1"), NEXT));
    assertEquals(2, count(fragments.get("AAllFields2"), NEXT));
    assertEquals(3, count(fragments.get("AAllFields3"), NEXT));
    assertEquals(1, count(fragments.get("AAllFields"), NEXT));
  }

  private static int count(String text, String word) {
    return text.split(word, -1).length - 1;
  }
}

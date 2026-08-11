package org.molgenis.emx2.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class JsonUtilTest {

  private static final String PARENT_SCHEMA = JsonUtilTest.class.getSimpleName() + "Parent";
  private static final String CROSS_SCHEMA = JsonUtilTest.class.getSimpleName() + "Cross";
  private static final String LOCAL_SCHEMA = JsonUtilTest.class.getSimpleName() + "Local";

  private static Schema crossSchemaChild;
  private static Schema localSchemaChild;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(CROSS_SCHEMA);
    database.dropSchemaIfExists(LOCAL_SCHEMA);
    database.dropSchemaIfExists(PARENT_SCHEMA);

    Schema parent = database.createSchema(PARENT_SCHEMA);
    parent.create(table("Shape").add(column("name").setPkey()));

    crossSchemaChild = database.createSchema(CROSS_SCHEMA);
    crossSchemaChild.create(
        table("MyShape")
            .setInheritName("Shape")
            .setImportSchema(PARENT_SCHEMA)
            .add(column("surface")));

    localSchemaChild = database.createSchema(LOCAL_SCHEMA);
    localSchemaChild.create(table("Shape").add(column("name").setPkey()));
    localSchemaChild.create(table("MyShape").setInheritName("Shape").add(column("surface")));
  }

  @Test
  void yamlExportIncludesInheritSchemaName() throws IOException {
    String yaml = JsonUtil.schemaToYaml(crossSchemaChild.getMetadata(), false);

    assertTrue(yaml.contains("inheritSchemaName: \"" + PARENT_SCHEMA + "\""), yaml);

    SchemaMetadata reparsed = JsonUtil.yamlToSchema(yaml);
    assertEquals("Shape", reparsed.getTableMetadata("MyShape").getInheritName());
    assertEquals(PARENT_SCHEMA, reparsed.getTableMetadata("MyShape").getImportSchema());
  }

  @Test
  void jsonToSchemaAcceptsChildListedBeforeItsParent() throws IOException {
    String json =
        """
        {"tables":[{"name":"Cat","inheritName":"Pet","columns":[{"name":"whiskers"}]},
                   {"name":"Pet","columns":[{"name":"id","key":1}]}]}""";

    SchemaMetadata parsed = JsonUtil.jsonToSchema(json);

    assertEquals("Pet", parsed.getTableMetadata("Cat").getInheritName());
    assertEquals("Pet", parsed.getTableMetadata("Cat").getInheritedTable().getTableName());
    assertTrue(parsed.getTableMetadata("Cat").getColumnNames().contains("id"), "inherits pkey");
  }

  @Test
  void yamlExportOmitsInheritSchemaNameForSameSchemaParent() throws IOException {
    String yaml = JsonUtil.schemaToYaml(localSchemaChild.getMetadata(), false);

    assertTrue(yaml.contains("inheritName: \"Shape\""), yaml);
    assertFalse(yaml.contains("inheritSchemaName"), yaml);
  }
}

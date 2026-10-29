package org.molgenis.emx2.json;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;

class DetachedSchemaJsonTest {

  @Test
  void detachedSchemaWithUnresolvableKeyRefSerialisesWithoutRowLabelTemplate() throws IOException {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        table(
            "Contacts",
            column("resource")
                .setType(ColumnType.REF)
                .setRefSchemaName("Other")
                .setRefTable("Resources")
                .setKey(1),
            column("name").setType(ColumnType.STRING).setKey(1)));

    String json = JsonUtil.schemaToJson(schema);

    assertTrue(json.contains("\"Contacts\""), json);
    assertFalse(json.contains("rowLabelTemplate"), json);
  }
}

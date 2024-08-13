package org.molgenis.emx2.io;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStoreForProfile;

public class ImportProfileTask extends ImportSchemaTask {
  public ImportProfileTask(Schema schema, String template, Boolean includeDemoData) {
    super(
        "Import profile:",
        new TableStoreForProfile(schema, template, includeDemoData),
        schema,
        false);
  }
}

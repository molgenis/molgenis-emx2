package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.beans.SchemaMetadata;

public class TestExportImportRoundtrip {

  @Test
  public void test() {
    Schema schema = new SchemaMetadata("test");

    // Table t = schema.createTableIfNotExists("test");
  }
}

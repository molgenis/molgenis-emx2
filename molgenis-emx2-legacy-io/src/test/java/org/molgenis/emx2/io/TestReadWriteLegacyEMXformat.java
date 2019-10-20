package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.legacy.format.Emx1ToSchema;

import java.io.File;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;

public class TestReadWriteLegacyEMXformat {

  @Test
  public void test() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("bbmri-nl").getFile());

    RowStoreForCsvFilesDirectory store = new RowStoreForCsvFilesDirectory(file.toPath(), ';');

    SchemaMetadata schema = Emx1ToSchema.convert(store, "bbmri_nl_");

    assertEquals(22, schema.getTableNames().size());
    System.out.println(schema.toString());
  }
}

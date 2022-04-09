package org.molgenis.emx2.io;

import java.nio.file.Path;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;

public class ImportCsvZipTask extends ImportSchemaTask {
  public ImportCsvZipTask(Path zipFile, Schema schema, boolean strict) {
    super("Import csv file", new TableStoreForCsvInZipFile(zipFile), schema, strict);
  }
}

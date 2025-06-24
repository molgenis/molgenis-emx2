package org.molgenis.emx2.io;

import java.nio.file.Path;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFilesDirectory;

public class ImportDirectoryTask extends ImportSchemaTask {
  public ImportDirectoryTask(Path directory, Schema schema, boolean strict) {
    super("Import directory", new TableStoreForCsvFilesDirectory(directory), schema, strict);
  }
}

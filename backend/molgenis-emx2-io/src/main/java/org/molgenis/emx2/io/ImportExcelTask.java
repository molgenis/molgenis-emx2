package org.molgenis.emx2.io;

import java.nio.file.Path;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;

/** Import schema task that uses Excel as table store */
public class ImportExcelTask extends ImportSchemaTask {
  public ImportExcelTask(Path excelFile, Schema schema, boolean strict) {
    super("Import excel file", new TableStoreForXlsxFile(excelFile), schema, strict);
  }
}

package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.harvester.util.HarvestingTestSchema;
import org.molgenis.emx2.io.ImportTableTask;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;

class TestDataImport {

  @Test
  void shouldUploadData() throws InterruptedException {
    SchemaMetadata schema = HarvestingTestSchema.create();
    TableStoreForCsvInMemory tableStore = new TableStoreForCsvInMemory();
    tableStore.setCsvString("Resources", readCsv());

    ImportTableTask importTableTask =
        new ImportTableTask(tableStore, schema.getTableMetadata("Resources").getTable(), false);
    importTableTask.run();

    System.out.println("Importing data...");
    while (importTableTask.isRunning()) {
      System.out.println("Running import data task...");
      Thread.sleep(3000);
    }

    System.out.println("Done uploading");
  }

  private static String readCsv() {
    try {
      return new String(
          Objects.requireNonNull(TestDataImport.class.getResourceAsStream("upload/Resources.csv"))
              .readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

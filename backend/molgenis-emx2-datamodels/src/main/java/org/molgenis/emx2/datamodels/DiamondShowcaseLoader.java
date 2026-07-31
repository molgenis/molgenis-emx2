package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.SchemaLoaderSettings;

public class DiamondShowcaseLoader extends ImportDataModelTask {

  public DiamondShowcaseLoader(SchemaLoaderSettings schemaLoaderSettings) {
    super(schemaLoaderSettings);
  }

  @Override
  public void run() {
    this.start();
    try {
      createSchema(getSchema(), "diamond_showcase/molgenis.csv");
      if (isIncludeDemoData()) {
        MolgenisIO.fromClasspathDirectory("diamond_showcase/data", getSchema(), false);
      }
      this.complete();
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw new MolgenisException("Failed to create diamond showcase schema", e);
    }
  }
}

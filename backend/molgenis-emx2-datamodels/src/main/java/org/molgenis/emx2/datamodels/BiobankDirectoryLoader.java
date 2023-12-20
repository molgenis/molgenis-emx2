package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class BiobankDirectoryLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    createSchema(schema, "biobank-directory/molgenis.csv");
    MolgenisIO.fromClasspathDirectory("biobank-directory/ontologies", schema, false);
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("biobank-directory/demo", schema, false);
    }
  }
}

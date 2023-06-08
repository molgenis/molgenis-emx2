package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class BBMRIEricLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    createSchema(schema, "bbmri-eric/molgenis.csv");
    MolgenisIO.fromClasspathDirectory("bbmri-eric/DirectoryOntologies", schema, false);
  }
}

package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class ErnCranioLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    createSchema(schema, "ern-cranio/model/molgenis.csv");
    MolgenisIO.fromClasspathDirectory("ern-cranio/ontologies", schema, false);
  }
}

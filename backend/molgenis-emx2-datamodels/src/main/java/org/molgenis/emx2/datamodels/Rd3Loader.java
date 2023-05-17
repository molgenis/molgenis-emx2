package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class Rd3Loader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    createSchema(schema, "rd3/rd3/molgenis.csv");
    MolgenisIO.fromClasspathDirectory("rd3/ontologies", schema, false);
  }
}


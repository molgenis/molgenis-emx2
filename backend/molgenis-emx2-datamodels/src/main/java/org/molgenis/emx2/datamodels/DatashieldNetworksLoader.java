package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class DatashieldNetworksLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema) {
    createSchema(schema, "datashield-networks-overview/molgenis.csv");


    MolgenisIO.fromClasspathDirectory("datashield-networks-overview/ontologies", schema, false);
    MolgenisIO.fromClasspathDirectory("datashield-networks-overview/datasets", schema, false);
  }
}

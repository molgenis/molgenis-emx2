package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.createSharedStaging;

import org.molgenis.emx2.Schema;

public class DataCatalogueNetworkStagingLoader extends AbstractDataLoader {

  @Override
  public void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedStaging(schema.getDatabase());

    // create the schema
    createSchema(schema, "datacatalogue/stagingNetworks/molgenis.csv");
  }
}

package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader3.createSharedStaging;

import org.molgenis.emx2.Schema;

public class DataCatalogueNetworkStagingLoader3 extends AbstractDataLoader {

  @Override
  public void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedStaging(schema.getDatabase());

    // create the schema
    createSchema(schema, "datacatalogue3/stagingNetworks/molgenis.csv");
  }
}

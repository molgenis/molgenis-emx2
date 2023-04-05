package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader3.createSharedStaging3;

import org.molgenis.emx2.Schema;

public class DataCatalogueNetworkStagingLoader3 extends AbstractDataLoader {

  @Override
  public void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedStaging3(schema.getDatabase());

    // create the schema
    createSchema(schema, "datacatalogue3/stagingNetworks/molgenis.csv");
  }
}

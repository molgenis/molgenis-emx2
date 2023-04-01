package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.createSharedSchema;

import org.molgenis.emx2.Schema;

public class DataCatalogueNetworkStagingLoader extends AbstractDataLoader {

  static final String SHARED_STAGING = "SharedStaging";

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedSchema(schema.getDatabase());

    // create the schema
    createSchema(schema, "datacatalogue/stagingNetworks/molgenis.csv");
  }
}

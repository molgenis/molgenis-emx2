package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.createSharedSchema;
import static org.molgenis.emx2.datamodels.DataCatalogueLoader.createSchema;

import org.molgenis.emx2.Schema;

public class DataCatalogueNetworkStagingLoader implements AvailableDataModels.DataModelLoader {

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedSchema(schema.getDatabase());

    // create the schema
    createSchema(schema, "datacatalogue/stagingNetworks/molgenis.csv");
  }
}

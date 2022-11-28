package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.createSharedSchema;
import static org.molgenis.emx2.datamodels.DataCatalogueLoader.createSchema;

public class DataCatalogueNetworkStagingLoader3 implements AvailableDataModels.DataModelLoader {

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedSchema(schema.getDatabase());

    // create the schema
    createSchema(schema, "datacatalogue3/stagingNetworks/molgenis.csv");
  }
}

package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader3.createDataCatalogue3;
import static org.molgenis.emx2.datamodels.DataCatalogueLoader.createSchema;

import org.molgenis.emx2.Schema;

public class DataCatalogueNetworkStagingLoader3 implements AvailableDataModels.DataModelLoader {

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createDataCatalogue3(schema.getDatabase());

    // create the schema
    createSchema(schema, "datacatalogue3/stagingNetworks/molgenis.csv");
  }
}

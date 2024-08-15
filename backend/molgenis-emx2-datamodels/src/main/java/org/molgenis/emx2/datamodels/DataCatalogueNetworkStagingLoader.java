package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.createSharedStaging;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;

public class DataCatalogueNetworkStagingLoader extends ImportDataModelTask {

  public DataCatalogueNetworkStagingLoader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    // create shared schemas
    createSharedStaging(getSchema());

    // create the schema
    createSchema(getSchema(), "datacatalogue/stagingNetworks/molgenis.csv");
    this.complete();
  }
}

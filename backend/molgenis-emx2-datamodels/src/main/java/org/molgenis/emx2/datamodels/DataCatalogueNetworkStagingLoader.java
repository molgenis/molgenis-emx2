package org.molgenis.emx2.datamodels;

//import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.createSharedStaging;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class DataCatalogueNetworkStagingLoader extends AbstractDataLoader {

  static String DATA_CATALOGUE = "catalogue";
  static final String SHARED_STAGING = "SharedStaging";

  @Override
  public void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedStaging(schema.getDatabase());
    // create the schema
    createSchema(schema, "datacatalogue/stagingNetworks/molgenis.csv");
  }

  static void createSharedStaging(Database db) {
    // create DataCatalogue and CatalogueOntologies
    Schema dataCatalogueSchema = db.getSchema(DATA_CATALOGUE);
    if (dataCatalogueSchema == null) {
      new DataCatalogueLoader().loadInternalImplementation(db.createSchema(DATA_CATALOGUE), false);
    }

    Schema sharedSchema = db.getSchema(SHARED_STAGING);
    if (sharedSchema == null) {
      createSchema(db.createSchema(SHARED_STAGING), "datacatalogue/stagingShared/molgenis.csv");
    }
  }
}

package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueNetworkStagingLoader.SHARED_STAGING;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class DataCatalogueCohortStagingLoader extends AbstractDataLoader {

  static String DATA_CATALOGUE = "DataCatalogue";

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedSchema(schema.getDatabase());
    // create the schema
    createSchema(schema, "datacatalogue/stagingCohorts/molgenis.csv");
  }

  static void createSharedSchema(Database db) {
    // create DataCatalogue and CatalogueOntologies
    Schema dataCatalogueSchema = db.getSchema(DATA_CATALOGUE);
    if (dataCatalogueSchema == null) {
      new DataCatalogueLoader().loadInternalImplementation(db.createSchema(DATA_CATALOGUE), false);
    }

    Schema sharedSchema = db.getSchema(SHARED_STAGING);
    if (sharedSchema == null) {
      sharedSchema = db.createSchema(SHARED_STAGING);
      // create the shared schema
      createSchema(sharedSchema, "datacatalogue/stagingShared/molgenis.csv");
    }
  }
}

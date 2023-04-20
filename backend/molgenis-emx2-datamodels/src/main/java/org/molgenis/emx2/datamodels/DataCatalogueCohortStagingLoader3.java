package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class DataCatalogueCohortStagingLoader3 extends AbstractDataLoader {

  static final String SHARED_STAGING = "SharedStaging";
  static String DATA_CATALOGUE = "DataCatalogue";

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedStaging3(schema.getDatabase());
    // create the schema
    createSchema(schema, "datacatalogue3/stagingCohorts/molgenis.csv");
  }

  static void createSharedStaging3(Database db) {
    // create DataCatalogue and CatalogueOntologies
    Schema dataCatalogueSchema = db.getSchema(DATA_CATALOGUE);
    if (dataCatalogueSchema == null) {
      new DataCatalogueLoader3().loadInternalImplementation(db.createSchema(DATA_CATALOGUE), false);
    }

    Schema sharedSchema = db.getSchema(SHARED_STAGING);
    if (sharedSchema == null) {
      createSchema(db.createSchema(SHARED_STAGING), "datacatalogue3/stagingShared/molgenis.csv");
    }
  }
}

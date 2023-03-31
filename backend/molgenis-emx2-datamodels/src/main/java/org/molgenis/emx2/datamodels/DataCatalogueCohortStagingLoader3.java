package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueLoader.createSchema;
import static org.molgenis.emx2.datamodels.DataCatalogueNetworkStagingLoader.SHARED_STAGING;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class DataCatalogueCohortStagingLoader3 implements AvailableDataModels.DataModelLoader {

  static String DATA_CATALOGUE = "DataCatalogue";

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createDataCatalogue3(schema.getDatabase());
    // create the schema
    createSchema(schema, "datacatalogue3/stagingCohorts/molgenis.csv");
  }

  static void createDataCatalogue3(Database db) {
    // create DataCatalogue and CatalogueOntologies
    Schema dataCatalogueSchema = db.getSchema(DATA_CATALOGUE);
    if (dataCatalogueSchema == null) {
      new DataCatalogueLoader3().load(db.createSchema(DATA_CATALOGUE), false);
    }

    Schema sharedSchema = db.getSchema(SHARED_STAGING);
    if (sharedSchema == null) {
      sharedSchema = db.createSchema(SHARED_STAGING);
      // create the shared schema
      createSchema(sharedSchema, "datacatalogue3/stagingShared/molgenis.csv");
    }
  }
}

package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;

public class DataCatalogueCohortStagingLoader extends ImportDataModelTask {

  static String DATA_CATALOGUE = "catalogue";
  static final String SHARED_STAGING = "SharedStaging";

  public DataCatalogueCohortStagingLoader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    // create shared schemas
    createSharedStaging(getSchema());
    // create the schema
    createSchema("datacatalogue/stagingCohorts/molgenis.csv");
    this.complete();
  }

  static void createSharedStaging(Schema schema) {
    // create DataCatalogue and CatalogueOntologies
    Database db = schema.getDatabase();
    Schema dataCatalogueSchema = db.getSchema(DATA_CATALOGUE);
    if (dataCatalogueSchema == null) {
      DataModels.Profile.DATA_CATALOGUE.getImportTask(db.createSchema(DATA_CATALOGUE), false).run();
    }

    Schema sharedSchema = db.getSchema(SHARED_STAGING);
    if (sharedSchema == null) {
      createSchema(db.createSchema(SHARED_STAGING), "datacatalogue/stagingShared/molgenis.csv");
    }
  }
}

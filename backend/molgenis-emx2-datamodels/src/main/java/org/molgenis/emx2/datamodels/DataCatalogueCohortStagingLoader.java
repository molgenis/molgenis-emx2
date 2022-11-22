package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueLoader.*;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class DataCatalogueCohortStagingLoader implements AvailableDataModels.DataModelLoader {

  static String DATA_CATALOGUE = "DataCatalogue";

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    // create shared schemas
    createSharedSchema(schema.getDatabase());
    // create the schema
    createSchema(schema, "datacatalogue/stagingCohorts/molgenis.csv");
  }

  static void createSharedSchema(Database db) {
    // create DataCatalogue and CatalogueOntologies
    Schema dataCatalogueSchema = db.getSchema(DATA_CATALOGUE);
    if (dataCatalogueSchema == null) {
      new DataCatalogueLoader().load(db.createSchema(DATA_CATALOGUE), false);
    }
  }
}

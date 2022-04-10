package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueLoader.*;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class DataCatalogueStagingLoader implements AvailableLoadersEnum.DataModelLoader {

  @Override
  public void load(Schema schema, boolean includeDemoData) {

    // depends on CatalogueOntologies schema, so we create that if missing
    Database db = schema.getDatabase();
    intitOntologies(db);

    // create the schema
    createSchema(schema, "datacatalogue/Catalogue_cdm/molgenis.csv");

    loadOntologies(db);
  }
}

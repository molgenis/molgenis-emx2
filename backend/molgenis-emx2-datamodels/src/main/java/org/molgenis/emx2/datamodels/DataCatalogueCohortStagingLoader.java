package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueLoader.*;
import static org.molgenis.emx2.datamodels.DataCatalogueNetworkStagingLoader.SHARED_STAGING;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class DataCatalogueCohortStagingLoader implements AvailableDataModels.DataModelLoader {

  static String DATA_CATALOGHUE = "DataCatalogue";

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    // create ontology schema
    Database db = schema.getDatabase();

    Schema ontologySchema = db.getSchema(CATALOGUE_ONTOLOGIES);
    if (ontologySchema == null) {
      ontologySchema = db.createSchema(CATALOGUE_ONTOLOGIES);
    }

    Schema dataCatalogueSchema = db.getSchema(DATA_CATALOGHUE);
    if (dataCatalogueSchema == null) {
      dataCatalogueSchema = db.createSchema(DATA_CATALOGHUE);
      createSchema(dataCatalogueSchema, "datacatalogue/molgenis.csv");
    }

    Schema sharedSchema = db.getSchema(SHARED_STAGING);
    if (sharedSchema == null) {
      sharedSchema = db.createSchema(SHARED_STAGING);
      // create the shared schema
      createSchema(sharedSchema, "datacatalogue/stagingShared/molgenis.csv");
    }

    // create the schema
    createSchema(schema, "datacatalogue/stagingCohorts/molgenis.csv");

    // load data into ontology schema
    MolgenisIO.fromClasspathDirectory("datacatalogue/CatalogueOntologies", ontologySchema, false);
  }
}

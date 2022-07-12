package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.DATA_CATALOGHUE;
import static org.molgenis.emx2.datamodels.DataCatalogueLoader.CATALOGUE_ONTOLOGIES;
import static org.molgenis.emx2.datamodels.DataCatalogueLoader.createSchema;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class DataCatalogueNetworkStagingLoader implements AvailableDataModels.DataModelLoader {

  static final String SHARED_STAGING = "SharedStaging";

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
    createSchema(schema, "datacatalogue/networksStaging/molgenis.csv");

    // load data into ontology schema
    MolgenisIO.fromClasspathDirectory("datacatalogue/CatalogueOntologies", ontologySchema, false);
  }
}

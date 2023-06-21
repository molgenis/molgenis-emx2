package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.DataCatalogueCohortStagingLoader.DATA_CATALOGUE;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;

public class DataCatalogueNetworkManagementLoader extends AbstractDataLoader {

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    // create DataCatalogue and CatalogueOntologies
    Schema dataCatalogueSchema = schema.getDatabase().getSchema(DATA_CATALOGUE);
    if (dataCatalogueSchema == null) {
      new DataCatalogueLoader()
          .loadInternalImplementation(schema.getDatabase().createSchema(DATA_CATALOGUE), false);
    }

    createSchema(schema, "datashield-networks-overview/molgenis.csv");

    MolgenisIO.fromClasspathDirectory("datashield-networks-overview/ontologies", schema, false);
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("datashield-networks-overview/datasets", schema, false);
    }
  }
}

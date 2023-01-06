package org.molgenis.emx2.datamodels;

import java.io.IOException;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

public enum AvailableDataModels {
  PET_STORE(new PetStoreLoader()),
  DATA_CATALOGUE(new DataCatalogueLoader()),
  FAIR_DATA_HUB(new FAIRDataHubLoader()),
  DIRECTORY(new DirectoryLoader()),
  DATA_CATALOGUE3(new DataCatalogueLoader3()),
  DATA_CATALOGUE_COHORT_STAGING(new DataCatalogueCohortStagingLoader()),
  DATA_CATALOGUE_NETWORK_STAGING(new DataCatalogueNetworkStagingLoader()),
  DATA_CATALOGUE_COHORT_STAGING3(new DataCatalogueCohortStagingLoader3()),
  DATA_CATALOGUE_NETWORK_STAGING3(new DataCatalogueNetworkStagingLoader3());

  private DataModelLoader installer;

  AvailableDataModels(DataModelLoader installer) {
    this.installer = installer;
  }

  public void install(Schema schema, boolean includeExampleData) {
    schema.tx(
        db -> {
          try {
            // make sure not to use schema but db because in a transaction!
            installer.load(db.getSchema(schema.getName()), includeExampleData);
          } catch (Exception e) {
            throw new MolgenisException(e.getMessage());
          }
        });
  }

  public interface DataModelLoader {
    void load(Schema schema, boolean includeExampleData) throws IOException;
  }
}

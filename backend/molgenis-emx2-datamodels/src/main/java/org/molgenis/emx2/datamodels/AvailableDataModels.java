package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;

public enum AvailableDataModels {
  DATA_CATALOGUE3(new DataCatalogueLoader3()),
  DATA_CATALOGUE_COHORT_STAGING3(new DataCatalogueCohortStagingLoader3()),
  DATA_CATALOGUE_NETWORK_STAGING3(new DataCatalogueNetworkStagingLoader3()),
  PET_STORE(new PetStoreLoader()),
  FAIR_DATA_HUB(new FAIRDataHubLoader());

  private AbstractDataLoader installer;

  AvailableDataModels(AbstractDataLoader installer) {
    this.installer = installer;
  }

  public void install(Schema schema, boolean loadExampleData) {
    this.installer.load(schema, loadExampleData);
  }
}

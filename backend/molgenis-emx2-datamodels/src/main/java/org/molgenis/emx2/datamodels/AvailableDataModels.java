package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;

public enum AvailableDataModels {
  DIRECTORY(new DirectoryLoader()),
  DATA_CATALOGUE_COHORT_STAGING(new DataCatalogueCohortStagingLoader()),
  DATA_CATALOGUE_NETWORK_STAGING(new DataCatalogueNetworkStagingLoader()),
  DATA_CATALOGUE(new DataCatalogueLoader()),
  PET_STORE(new PetStoreLoader()),
  FAIR_DATA_HUB(new FAIRDataHubLoader()),
  RD3(new Rd3Loader()),
  DASHBOARD(new DashboardLoader()),
  BIOBANK_DIRECTORY(new BiobankDirectoryLoader()),
  DATA_CATALOGUE_NETWORK_MANAGEMENT(new DataCatalogueNetworkManagementLoader());

  private AbstractDataLoader installer;

  AvailableDataModels(AbstractDataLoader installer) {
    this.installer = installer;
  }

  public void install(Schema schema, boolean loadExampleData) {
    this.installer.load(schema, loadExampleData);
  }
}

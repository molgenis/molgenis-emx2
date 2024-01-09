package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;

public enum AvailableDataModels {
  DIRECTORY(new DirectoryLoader()),
  DATA_CATALOGUE_COHORT_STAGING(new DataCatalogueCohortStagingLoader()),
  DATA_CATALOGUE_NETWORK_STAGING(new DataCatalogueNetworkStagingLoader()),
  DATA_CATALOGUE(new DataCatalogueLoader()),
  PET_STORE(new PetStoreLoader()),
  FAIR_DATA_HUB(new ProfileLoader("fairdatahub/FAIRDataHub.yaml")),
  RD3(new ProfileLoader("fairdatahub/RD3.yaml")),
  JRC_COMMON_DATA_ELEMENTS(new ProfileLoader("fairdatahub/JRC-CDE.yaml")),
  BEACON_V2(new ProfileLoader("fairdatahub/BeaconV2.yaml")),
  ERN_DASHBOARD(new DashboardLoader()),
  ERN_CRANIO(new ErnCranioLoader()),
  BIOBANK_DIRECTORY(new BiobankDirectoryLoader());

  private AbstractDataLoader installer;

  AvailableDataModels(AbstractDataLoader installer) {
    this.installer = installer;
  }

  public void install(Schema schema, boolean loadExampleData) {
    this.installer.load(schema, loadExampleData);
  }
}

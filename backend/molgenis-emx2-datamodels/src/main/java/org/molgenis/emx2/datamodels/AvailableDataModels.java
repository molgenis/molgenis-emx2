package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;

public enum AvailableDataModels {
  DIRECTORY(new DirectoryLoader()),
  DATA_CATALOGUE_COHORT_STAGING(new DataCatalogueCohortStagingLoader()),
  DATA_CATALOGUE_NETWORK_STAGING(new DataCatalogueNetworkStagingLoader()),
  DATA_CATALOGUE(new ProfileLoader("_profiles/DataCatalogue.yaml")),
  PET_STORE(new PetStoreLoader()),
  FAIR_DATA_HUB(new ProfileLoader("_profiles/FAIRDataHub.yaml")),
  RD3(new ProfileLoader("_profiles/RD3.yaml")),
  JRC_COMMON_DATA_ELEMENTS(new ProfileLoader("_profiles/JRC-CDE.yaml")),
  FAIR_GENOMES(new ProfileLoader("_profiles/FAIRGenomes.yaml")),
  BEACON_V2(new ProfileLoader("_profiles/BeaconV2.yaml")),
  CAFE_VARIOME(new ProfileLoader("_profiles/CafeVariome.yaml")),
  ERN_DASHBOARD(new DashboardLoader()),
  ERN_CRANIO(new ErnCranioLoader()),
  BIOBANK_DIRECTORY(new BiobankDirectoryLoader()),
  SHARED_STAGING(new ProfileLoader("_profiles/SharedStaging.yaml"));

  private AbstractDataLoader installer;

  AvailableDataModels(AbstractDataLoader installer) {
    this.installer = installer;
  }

  public void install(Schema schema, boolean loadExampleData) {
    this.installer.load(schema, loadExampleData);
  }
}

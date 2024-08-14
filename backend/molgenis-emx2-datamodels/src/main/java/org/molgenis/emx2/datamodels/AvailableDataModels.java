package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;

public enum AvailableDataModels {
  DIRECTORY(new DirectoryLoader()),
  DATA_CATALOGUE_COHORT_STAGING(new ProfileLoader("_profiles/CohortStaging.yaml")),
  DATA_CATALOGUE_NETWORK_STAGING(new DataCatalogueNetworkStagingLoader()),
  DATA_CATALOGUE_AGGREGATES(new ProfileLoader("_profiles/DataCatalogueAggregates.yaml")),
  DATA_CATALOGUE(new ProfileLoader("_profiles/DataCatalogue.yaml")),
  DATA_CATALOGUE_FLAT(new ProfileLoader("_profiles/DataCatalogueFlat.yaml")),
  PET_STORE(new PetStoreLoader()),
  FAIR_DATA_HUB(new ProfileLoader("_profiles/FAIRDataHub.yaml")),
  RD3(new ProfileLoader("_profiles/RD3.yaml")),
  JRC_COMMON_DATA_ELEMENTS(new ProfileLoader("_profiles/JRC-CDE.yaml")),
  FAIR_GENOMES(new ProfileLoader("_profiles/FAIRGenomes.yaml")),
  DCAT(new ProfileLoader("_profiles/DCAT.yaml")),
  FAIR_DATA_POINT(new ProfileLoader("_profiles/FAIRDataPoint.yaml")),
  BEACON_V2(new ProfileLoader("_profiles/BeaconV2.yaml")),
  ERN_DASHBOARD(new DashboardLoader()),
  BIOBANK_DIRECTORY(new BiobankDirectoryLoader(false)),
  BIOBANK_DIRECTORY_STAGING(new BiobankDirectoryLoader(true)),
  PROJECTMANAGER(new ProjectManagerLoader()),
  GDI(new ProfileLoader("_profiles/GDI.yaml")),
  SHARED_STAGING(new ProfileLoader("_profiles/SharedStaging.yaml")),
  FLAT_COHORTS_STAGING(new ProfileLoader("_profiles/CohortsStagingFLat.yaml")),
  FLAT_UMCG_COHORTS_STAGING(new ProfileLoader("_profiles/UMCGCohortsStagingFlat.yaml")),
  FLAT_STUDIES_STAGING(new ProfileLoader("_profiles/StudiesStagingFlat.yaml")),
  FLAT_NETWORKS_STAGING(new ProfileLoader("_profiles/NetworksStagingFlat.yaml")),
  FLAT_RWE_STAGING(new ProfileLoader("_profiles/RWEStagingFlat.yaml"));

  private AbstractDataLoader installer;

  AvailableDataModels(AbstractDataLoader installer) {
    this.installer = installer;
  }

  public void install(Schema schema, boolean loadExampleData) {
    this.installer.load(schema, loadExampleData);
  }
}

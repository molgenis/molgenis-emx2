package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.tasks.Task;

public class DataModels {

  public static Task getTask(Schema schema, String template, boolean includeDemoData) {
    Profile profile = Profile.valueOf(template);
    return profile.getTask(schema, includeDemoData);
  }

  public interface DataModelTask {
    Task getTask(Schema schema, boolean includeDemoData);
  }

  public enum Profile implements DataModelTask {
    DATA_CATALOGUE_COHORT_STAGING("_profiles/CohortStaging.yaml"),
    DATA_CATALOGUE_AGGREGATES("_profiles/DataCatalogueAggregates.yaml"),
    DATA_CATALOGUE("_profiles/DataCatalogue.yaml"),
    DATA_CATALOGUE_FLAT("_profiles/DataCatalogueFlat.yaml"),
    FAIR_DATA_HUB("_profiles/FAIRDataHub.yaml"),
    RD3("_profiles/RD3.yaml"),
    JRC_COMMON_DATA_ELEMENTS("_profiles/JRC-CDE.yaml"),
    FAIR_GENOMES("_profiles/FAIRGenomes.yaml"),
    DCAT("_profiles/DCAT.yaml"),
    FAIR_DATA_POINT("_profiles/FAIRDataPoint.yaml"),
    BEACON_V2("_profiles/BeaconV2.yaml"),
    GDI("_profiles/GDI.yaml"),
    SHARED_STAGING("_profiles/SharedStaging.yaml"),
    FLAT_COHORTS_STAGING("_profiles/CohortsStaging.yaml"),
    FLAT_UMCG_COHORTS_STAGING("_profiles/UMCGCohortsStaging.yaml"),
    FLAT_STUDIES_STAGING("_profiles/StudiesStaging.yaml"),
    FLAT_NETWORKS_STAGING("_profiles/NetworksStaging.yaml"),
    FLAT_RWE_STAGING("_profiles/RWEStaging.yaml");

    Profile(String template) {
      this.template = template;
    }

    private final String template;

    private String getTemplate() {
      return template;
    }

    public Task getTask(Schema schema, boolean includeDemoData) {
      return new ImportProfileTask(schema, this.getTemplate(), includeDemoData);
    }
  }

  public enum Regular implements DataModelTask {
    DIRECTORY(null),
    DATA_CATALOGUE_NETWORK_STAGING(null),
    PET_STORE(null),
    ERN_DASHBOARD(null),
    BIOBANK_DIRECTORY(null),
    BIOBANK_DIRECTORY_STAGING(null),
    PROJECTMANAGER(null);

    Regular(Task task) {
      this.task = task;
    }

    private final Task task;

    public Task getTask(Schema schema, boolean includeDemoData) {
      return task;
    }
  }
}

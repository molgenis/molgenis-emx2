package org.molgenis.emx2.datamodels;

import java.util.Arrays;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.tasks.Task;

public class DataModels {

  public enum Profile {
    DATA_CATALOGUE_COHORT_STAGING("_profiles/CohortStaging.yaml"),
    DATA_CATALOGUE_AGGREGATES("_profiles/DataCatalogueAggregates.yaml"),
    DATA_CATALOGUE("_profiles/DataCatalogue.yaml"),
    FAIR_DATA_HUB("_profiles/FAIRDataHub.yaml"),
    RD3("_profiles/RD3.yaml"),
    JRC_COMMON_DATA_ELEMENTS("_profiles/JRC-CDE.yaml"),
    FAIR_GENOMES("_profiles/FAIRGenomes.yaml"),
    DCAT("_profiles/DCAT.yaml"),
    FAIR_DATA_POINT("_profiles/FAIRDataPoint.yaml"),
    BEACON_V2("_profiles/BeaconV2.yaml"),
    GDI("_profiles/GDI.yaml"),
    SHARED_STAGING("_profiles/SharedStaging.yaml");

    public static boolean hasProfile(String nameOther) {
      return Arrays.stream(values()).anyMatch(profile -> profile.name().equals(nameOther));
    }

    Profile(String template) {
      this.template = template;
    }

    private final String template;

    private String getTemplate() {
      return template;
    }

    public Task getImportTask(Schema schema, boolean includeDemoData) {
      return new ImportProfileTask(schema, this.getTemplate(), includeDemoData);
    }
  }

  public enum Regular {
    DIRECTORY(DirectoryLoader::new),
    DATA_CATALOGUE_NETWORK_STAGING(DataCatalogueNetworkStagingLoader::new),
    PET_STORE(PetStoreLoader::new),
    ERN_DASHBOARD(DashboardLoader::new),
    PROJECTMANAGER(ProjectManagerLoader::new),
    BIOBANK_DIRECTORY(BiobankDirectoryLoader::new),
    BIOBANK_DIRECTORY_STAGING(
        ((schema, includeDemoData) ->
            new BiobankDirectoryLoader(schema, includeDemoData).setStaging(true)));

    @FunctionalInterface
    private interface TaskFactory {
      ImportDataModelTask createTask(Schema schema, boolean includeDemoData);
    }

    private final TaskFactory taskFactory;

    Regular(TaskFactory taskFactory) {
      this.taskFactory = taskFactory;
    }

    public Task getImportTask(Schema schema, boolean includeDemoData) {
      return taskFactory.createTask(schema, includeDemoData);
    }
  }

  public static Task getImportTask(Schema schema, String template, boolean includeDemoData) {
    Task task;
    if (Profile.hasProfile(template)) {
      Profile profile = Profile.valueOf(template);
      task = profile.getImportTask(schema, includeDemoData);
    } else {
      task = Regular.valueOf(template).getImportTask(schema, includeDemoData);
    }
    return task.setDescription("Loading data model: " + template + " onto " + schema.getName());
  }
}

package org.molgenis.emx2.datamodels;

import java.util.Arrays;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.io.SchemaLoaderSettings;
import org.molgenis.emx2.tasks.Task;

public class DataModels {

  public enum Profile {
    DATA_CATALOGUE("_profiles/DataCatalogue.yaml"),
    DATA_CATALOGUE_COHORT_STAGING("_profiles/CohortsStaging.yaml"),
    DATA_CATALOGUE_NETWORK_STAGING("_profiles/NetworksStaging.yaml"),
    DATA_CATALOGUE_AGGREGATES("_profiles/DataCatalogueAggregates.yaml"),
    UMCG_COHORT_STAGING("_profiles/UMCGCohortsStaging.yaml"),
    UMCU_COHORTS_STAGING("_profiles/UMCUCohorts.yaml"),
    INTEGRATE_COHORTS_STAGING("_profiles/INTEGRATECohorts.yaml"),
    FAIR_DATA_HUB("_profiles/FAIRDataHub.yaml"),
    PATIENT_REGISTRY("_profiles/PatientRegistry.yaml"),
    FAIR_GENOMES("_profiles/FAIRGenomes.yaml"),
    FAIR_DATA_POINT("_profiles/FAIRDataPoint.yaml"),
    BEACON_V2("_profiles/BeaconV2.yaml"),
    SHARED_STAGING("_profiles/SharedStaging.yaml"),
    IMAGE_TEST("_profiles/ImageTest.yaml"),
    PET_STORE("_profiles/PetStore.yaml"),
    TYPE_TEST("_profiles/TypeTest.yaml"),
    MG_CMS("_profiles/Pages.yaml");

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

    public Task getImportTask(
        Database database, String schemaName, String description, boolean includeDemoData) {
      return new ImportProfileTask(
          database, schemaName, description, this.getTemplate(), includeDemoData);
    }
  }

  public enum Regular {
    ERN_DASHBOARD(DashboardLoader::new),
    UI_DASHBOARD(UiDashboardLoader::new),
    PATIENT_REGISTRY_DEMO(PatientRegistryDemoLoader::new),
    PROJECTMANAGER(ProjectManagerLoader::new),
    BIOBANK_DIRECTORY(BiobankDirectoryLoader::new),
    BIOBANK_DIRECTORY_STAGING(
        (schemaLoaderSettings ->
            new BiobankDirectoryLoader(schemaLoaderSettings).setStaging(true)));

    public static boolean hasRegular(String nameOther) {
      return Arrays.stream(values()).anyMatch(regular -> regular.name().equals(nameOther));
    }

    @FunctionalInterface
    private interface TaskFactory {
      ImportDataModelTask createTask(SchemaLoaderSettings schemaLoaderSettings);
    }

    private final TaskFactory taskFactory;

    Regular(TaskFactory taskFactory) {
      this.taskFactory = taskFactory;
    }

    public Task getImportTask(
        Database database, String schemaName, String description, boolean includeDemoData) {
      SchemaLoaderSettings schemaLoaderSettings =
          new SchemaLoaderSettings(database, schemaName, description, includeDemoData);
      return taskFactory.createTask(schemaLoaderSettings);
    }

    public Task getImportTask(SchemaLoaderSettings schemaLoaderSettings) {
      return taskFactory.createTask(schemaLoaderSettings);
    }
  }

  public static Task getImportTask(
      Database database,
      String schemaName,
      String description,
      String template,
      boolean includeDemoData) {
    Task task;
    if (Profile.hasProfile(template)) {
      Profile profile = Profile.valueOf(template);
      task = profile.getImportTask(database, schemaName, description, includeDemoData);
    } else if (Regular.hasRegular(template)) {
      task =
          Regular.valueOf(template)
              .getImportTask(
                  new SchemaLoaderSettings(database, schemaName, description, includeDemoData));
    } else {
      throw new MolgenisException("Cannot create schema from template '" + template + "'.");
    }
    return task.setDescription("Loading data model: " + template + " onto " + schemaName);
  }
}

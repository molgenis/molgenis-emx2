package org.molgenis.emx2.datamodels;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.io.ImportBundleTask;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.io.SchemaLoaderSettings;
import org.molgenis.emx2.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataModels {

  private static final Logger log = LoggerFactory.getLogger(DataModels.class);
  private static final String MOLGENIS_YAML = "molgenis.yaml";
  private static List<BundleInfo> cachedBundles = null;

  private DataModels() {}

  public static void clearBundleCache() {
    cachedBundles = null;
  }

  public record BundleInfo(String id, String name, String description, String path) {}

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

  public static List<BundleInfo> discoverBundles() {
    if (cachedBundles != null) {
      return cachedBundles;
    }
    List<BundleInfo> result = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    ClassLoader classLoader = DataModels.class.getClassLoader();
    try {
      Enumeration<URL> resources = classLoader.getResources(MOLGENIS_YAML);
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        try {
          BundleInfo info = readBundleInfo(url, mapper, classLoader);
          if (info != null) {
            result.add(info);
          }
        } catch (Exception e) {
          log.trace("Skipping bundle at {}: {}", url, e.getMessage());
        }
      }
    } catch (IOException e) {
      log.warn("Failed to discover bundles: {}", e.getMessage());
    }
    cachedBundles = result;
    return result;
  }

  @SuppressWarnings("unchecked")
  private static BundleInfo readBundleInfo(URL url, ObjectMapper mapper, ClassLoader classLoader)
      throws IOException {
    String bundlePath = deriveBundlePath(url, classLoader);
    if (bundlePath == null) {
      return null;
    }
    try (InputStream inputStream = url.openStream()) {
      java.util.Map<String, Object> yaml = mapper.readValue(inputStream, java.util.Map.class);
      String name = (String) yaml.get("name");
      String description = (String) yaml.get("description");
      if (name == null) {
        return null;
      }
      String id = bundlePath.replace("/", "_").replace(".yaml", "");
      return new BundleInfo(id, name, description, bundlePath + "/");
    }
  }

  private static String deriveBundlePath(URL url, ClassLoader classLoader) {
    try {
      URI uri = url.toURI();
      String path = uri.getSchemeSpecificPart();
      if ("jar".equals(uri.getScheme())) {
        String entry = path.substring(path.indexOf('!') + 2);
        if (!entry.contains("/")) {
          return null;
        }
        return entry.substring(0, entry.lastIndexOf('/'));
      } else {
        String molgenisYamlResourcePath = "/" + MOLGENIS_YAML;
        URL rootUrl = classLoader.getResource(MOLGENIS_YAML);
        if (rootUrl != null && rootUrl.toURI().equals(uri)) {
          return null;
        }
        Enumeration<URL> all = classLoader.getResources(MOLGENIS_YAML);
        while (all.hasMoreElements()) {
          URL candidate = all.nextElement();
          if (candidate.toURI().equals(uri)) {
            String candidatePath = candidate.getPath();
            int lastSlash = candidatePath.lastIndexOf('/');
            if (lastSlash <= 0) return null;
            String dirPath = candidatePath.substring(0, lastSlash);
            int secondLastSlash = dirPath.lastIndexOf('/');
            return secondLastSlash >= 0 ? dirPath.substring(secondLastSlash + 1) : dirPath;
          }
        }
        return null;
      }
    } catch (Exception e) {
      log.trace("Cannot derive bundle path from {}: {}", url, e.getMessage());
      return null;
    }
  }

  public static List<BundleInfo> listAllModels() {
    List<BundleInfo> result = new ArrayList<>(discoverBundles());
    for (Profile profile : Profile.values()) {
      result.add(
          new BundleInfo(profile.name(), "Legacy: " + profile.name(), null, profile.getTemplate()));
    }
    for (Regular regular : Regular.values()) {
      result.add(new BundleInfo(regular.name(), "Legacy: " + regular.name(), null, null));
    }
    return result;
  }

  public static Task getImportTask(
      Database database,
      String schemaName,
      String description,
      String template,
      boolean includeDemoData) {
    Task task;
    BundleInfo bundle = findBundle(template);
    if (bundle != null) {
      task =
          new ImportBundleTask(database, schemaName, description, bundle.path(), includeDemoData);
    } else if (Profile.hasProfile(template)) {
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

  private static BundleInfo findBundle(String template) {
    for (BundleInfo bundle : discoverBundles()) {
      if (bundle.id().equals(template)) {
        return bundle;
      }
    }
    return null;
  }
}

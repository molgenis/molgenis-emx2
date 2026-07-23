package org.molgenis.emx2.datamodels;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.SchemaLoaderSettings;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.emx2.Emx2YamlBundle;

/**
 * Loads the diamond + companion schema YAML showcase (ticket 14): a diamond inheritance hierarchy
 * (Subject/ClinicalSubject/ResearchSubject/ClinicalResearchSubject) whose ontology columns carry a
 * dotted refTable into the fixed-name companion schema "diamontologies", provisioned once and
 * reused by later applies of this template.
 */
public class DiamondShowcaseYamlLoader extends ImportDataModelTask {

  private static final String BASE = "diamond_showcase/yaml/";
  private static final String COMPANION_BASE = BASE + "diamontologies/";
  private static final String COMPANION_SCHEMA_NAME = "diamontologies";

  private static final String[] ROOT_FILES = {
    "molgenis.yaml",
    "tables/Subject.yaml",
    "tables/ClinicalSubject.yaml",
    "tables/ResearchSubject.yaml",
    "tables/ClinicalResearchSubject.yaml"
  };

  private static final String[] COMPANION_FILES = {
    "molgenis.yaml", "tables/DiseaseCategories.yaml", "tables/AssayCategories.yaml"
  };

  public DiamondShowcaseYamlLoader(SchemaLoaderSettings schemaLoaderSettings) {
    super(schemaLoaderSettings);
  }

  @Override
  public void run() {
    this.start();
    try {
      Database database = getSchema().getDatabase();
      Schema companionSchema = database.getSchema(COMPANION_SCHEMA_NAME);
      if (companionSchema == null) {
        Emx2YamlBundle companionBundle =
            Emx2Yaml.fromBundleFiles(readClasspathFiles(COMPANION_BASE, COMPANION_FILES));
        companionSchema =
            database.createSchema(
                COMPANION_SCHEMA_NAME, "Companion ontologies for the diamond showcase");
        companionSchema.migrate(companionBundle.schema());
      }

      Emx2YamlBundle rootBundle = Emx2Yaml.fromBundleFiles(readClasspathFiles(BASE, ROOT_FILES));
      getSchema().migrate(rootBundle.schema());

      if (isIncludeDemoData()) {
        MolgenisIO.fromClasspathDirectory(COMPANION_BASE + "data", companionSchema, false);
        MolgenisIO.fromClasspathDirectory(BASE + "data", getSchema(), false);
      }
      this.complete();
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw new MolgenisException("Failed to create diamond showcase (YAML) schema", e);
    }
  }

  private static Map<String, String> readClasspathFiles(String base, String[] relativePaths) {
    Map<String, String> files = new LinkedHashMap<>();
    for (String relativePath : relativePaths) {
      files.put(relativePath, readClasspathFile(base + relativePath));
    }
    return files;
  }

  private static String readClasspathFile(String resourcePath) {
    try (InputStream stream =
        DiamondShowcaseYamlLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new MolgenisException("Failed to read model resource: " + resourcePath);
      }
      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        StringBuilder builder = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1) {
          builder.append((char) character);
        }
        return builder.toString();
      }
    } catch (MolgenisException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new MolgenisException("Failed to read model resource: " + resourcePath, exception);
    }
  }
}

package org.molgenis.emx2.datamodels.profiles;

import static org.molgenis.emx2.datamodels.profiles.PostProcessProfiles.csvStringToList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Yaml;

/**
 * YAML-backed counterpart of {@link SchemaFromProfile}: slices the reconverted YAML model corpus
 * (see {@link Emx2CorpusYamlConverter}) for a profile, yielding a {@link SchemaMetadata} equal to
 * the CSV-sliced result. Additive to the CSV path, which stays authoritative.
 */
public class SchemaFromProfileYaml {

  public static final String CSV_PARITY_YAML_DIR = "/yaml-format-test/csv-parity";

  private static final String MOLGENIS_YAML = "molgenis.yaml";
  private static final String PROFILES = "profiles";

  private final Profiles profiles;

  public SchemaFromProfileYaml(String yamlFileLocation) {
    if (!yamlFileLocation.endsWith(".yaml")) {
      throw new MolgenisException("Input YAML file name must end in '.yaml'");
    }
    InputStreamReader yaml =
        new InputStreamReader(
            Objects.requireNonNull(
                ImportDataModelTask.class.getClassLoader().getResourceAsStream(yamlFileLocation)));
    try {
      this.profiles = new ObjectMapper(new YAMLFactory()).readValue(yaml, Profiles.class);
    } catch (Exception exception) {
      throw new MolgenisException(exception.getMessage(), exception);
    }
  }

  public Profiles getProfiles() {
    return profiles;
  }

  public SchemaMetadata create() throws MolgenisException {
    return Emx2.fromRowList(createRows());
  }

  public List<Row> createRows() throws MolgenisException {
    List<String> selected = this.profiles.getProfileTagsList();
    List<Row> keepRows = new ArrayList<>();
    try {
      keepRows.addAll(sliceModelsInDirectory(CSV_PARITY_YAML_DIR, selected));
    } catch (URISyntaxException | IOException exception) {
      throw new MolgenisException(exception.getMessage(), exception);
    }
    return keepRows;
  }

  private static List<Row> sliceModelsInDirectory(String directory, List<String> selected)
      throws URISyntaxException, IOException {
    List<Row> keepRows = new ArrayList<>();
    if (SchemaFromProfileYaml.class.getResource(directory) == null) {
      return keepRows;
    }
    String[] modelsList = new ResourceListing().retrieve(directory);
    for (String modelLoc :
        Arrays.stream(modelsList).filter(model -> model.endsWith(".yaml")).sorted().toList()) {
      String content = readResource(directory + "/" + modelLoc);
      SchemaMetadata schema = Emx2Yaml.fromBundleFiles(Map.of(MOLGENIS_YAML, content)).schema();
      for (Row row : Emx2.toRowList(schema)) {
        if (matchesProfile(row, selected)) {
          keepRows.add(row);
        }
      }
    }
    return keepRows;
  }

  private static boolean matchesProfile(Row row, List<String> selected) {
    if (selected.isEmpty()) {
      return true;
    }
    List<String> rowProfiles = csvStringToList(row.getString(PROFILES));
    for (String profile : rowProfiles) {
      if (selected.contains(profile)) {
        return true;
      }
    }
    return false;
  }

  private static String readResource(String resource) {
    try (InputStreamReader reader =
        new InputStreamReader(
            Objects.requireNonNull(SchemaFromProfileYaml.class.getResourceAsStream(resource)))) {
      StringBuilder builder = new StringBuilder();
      int character;
      while ((character = reader.read()) != -1) {
        builder.append((char) character);
      }
      return builder.toString();
    } catch (Exception exception) {
      throw new MolgenisException("Failed to read model resource: " + resource, exception);
    }
  }
}

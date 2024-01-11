package org.molgenis.emx2.datamodels.profiles;

import static org.molgenis.emx2.datamodels.profiles.PostProcessProfiles.csvStringToList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.AbstractDataLoader;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class SchemaFromProfile {

  public static final String SHARED_MODELS_DIR =
      File.separator + "_models" + File.separator + "shared";
  public static final String SPECIFIC_MODELS_DIR =
      File.separator + "_models" + File.separator + "specific";

  private final Profiles profiles;

  /** Constructor without specific YAML file when you want everything */
  public SchemaFromProfile() {
    this.profiles = new Profiles();
  }

  /** Constructor with a specific profile YAML file */
  public SchemaFromProfile(String yamlFileLocation) {
    if (!yamlFileLocation.endsWith(".yaml")) {
      throw new MolgenisException("Input YAML file name must end in '.yaml'");
    }
    InputStreamReader yaml =
        new InputStreamReader(
            Objects.requireNonNull(
                AbstractDataLoader.class.getClassLoader().getResourceAsStream(yamlFileLocation)));
    try {
      this.profiles = new ObjectMapper(new YAMLFactory()).readValue(yaml, Profiles.class);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
  }

  public Profiles getProfiles() {
    return profiles;
  }

  public SchemaMetadata create() throws MolgenisException {
    return create(true);
  }

  public SchemaMetadata create(boolean filterByProfiles) throws MolgenisException {
    List<Row> keepRows = new ArrayList<>();
    try {
      keepRows.addAll(getProfilesFromAllModels(SHARED_MODELS_DIR, filterByProfiles));
      keepRows.addAll(getProfilesFromAllModels(SPECIFIC_MODELS_DIR, filterByProfiles));
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
    return Emx2.fromRowList(keepRows);
  }

  /** From a classpath dir, get all EMX2 model files and optionally slice for profiles */
  public List<Row> getProfilesFromAllModels(String directory, boolean filterByProfiles)
      throws URISyntaxException, IOException {
    List<Row> keepRows = new ArrayList<>();
    String[] modelsList = new ResourceListing().retrieve(directory);
    for (String schemaLoc : modelsList) {
      Iterable<Row> rowIterable =
          CsvTableReader.read(
              new InputStreamReader(
                  Objects.requireNonNull(
                      getClass().getResourceAsStream(directory + File.separator + schemaLoc))));

      for (Row row : rowIterable) {
        List<String> profiles = csvStringToList(row.getString("profiles"));
        if (profiles.isEmpty()) {
          throw new MolgenisException("No profiles for " + row);
        }
        for (String profile : profiles) {
          if (!filterByProfiles || this.profiles.getProfileTagsList().contains(profile)) {
            keepRows.add(row);
            break;
          }
        }
      }
    }
    return keepRows;
  }
}

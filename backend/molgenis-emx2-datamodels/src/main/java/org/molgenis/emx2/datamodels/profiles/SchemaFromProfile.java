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
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class SchemaFromProfile {

  public static final String SHARED_MODELS_DIR = "/_models/shared";
  public static final String SPECIFIC_MODELS_DIR = "/_models/specific";

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
                ImportDataModelTask.class.getClassLoader().getResourceAsStream(yamlFileLocation)));
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

  public Map<String, List<Row>> createRowsPerTable() throws MolgenisException {
    return createRowsPerTable(true);
  }

  public Map<String, List<Row>> createRowsPerTable(boolean filterByProfiles)
      throws MolgenisException {
    List<Row> createRows = createRows(filterByProfiles);
    Map<String, List<Row>> rowsPerTable = new HashMap<>();
    for (Row row : createRows) {
      String tableName = row.getString("tableName");
      if (!rowsPerTable.containsKey(tableName)) {
        rowsPerTable.put(tableName, new ArrayList<>());
      }
      rowsPerTable.get(tableName).add(row);
    }
    return rowsPerTable;
  }

  public SchemaMetadata create(boolean filterByProfiles) throws MolgenisException {
    return Emx2.fromRowList(createRows(filterByProfiles));
  }

  public List<Row> createRows(boolean filterByProfiles) throws MolgenisException {
    List<Row> keepRows = new ArrayList<>();
    try {
      keepRows.addAll(
          getProfilesFromAllModels(SHARED_MODELS_DIR, this.profiles.getProfileTagsList()));
      keepRows.addAll(
          getProfilesFromAllModels(SPECIFIC_MODELS_DIR, this.profiles.getProfileTagsList()));
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
    return keepRows;
  }

  /** From a classpath dir, get all EMX2 model files and optionally slice for profiles */
  public static List<Row> getProfilesFromAllModels(String directory, List<String> profilesSelected)
      throws URISyntaxException, IOException {
    List<Row> keepRows = new ArrayList<>();
    String[] modelsList = new ResourceListing().retrieve(directory);
    for (String schemaLoc :
        Arrays.stream(modelsList).filter(model -> !model.endsWith(".md")).toList()) {
      Iterable<Row> rowIterable =
          CsvTableReader.read(
              new InputStreamReader(
                  Objects.requireNonNull(
                      SchemaFromProfile.class.getResourceAsStream(directory + "/" + schemaLoc))));

      for (Row row : rowIterable) {
        if (profilesSelected.size() == 0) {
          keepRows.add(row);
        } else {
          List<String> profiles = csvStringToList(row.getString("profiles"));
          if (profiles.isEmpty()) {
            throw new MolgenisException("No profiles for " + row + " in file " + schemaLoc);
          }
          for (String profile : profiles) {
            if (profilesSelected.contains(profile)) {
              keepRows.add(row);
              break;
            }
          }
        }
      }
    }
    return keepRows;
  }
}

package org.molgenis.emx2.datamodels.profiles;

import static org.molgenis.emx2.datamodels.profiles.PostProcessProfiles.csvStringToList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.AbstractDataLoader;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class SchemaFromProfile {

  private String yamlFileLocation;
  private Profiles profiles;
  private Character separator;

  public SchemaFromProfile(String yamlFileLocation) {
    if (!yamlFileLocation.endsWith(".yaml")) {
      throw new MolgenisException("Input YAML file name must end in '.yaml'");
    }
    this.yamlFileLocation = yamlFileLocation;
    InputStreamReader yaml =
        new InputStreamReader(
            Objects.requireNonNull(
                AbstractDataLoader.class
                    .getClassLoader()
                    .getResourceAsStream(this.yamlFileLocation)));
    try {
      this.profiles = new ObjectMapper(new YAMLFactory()).readValue(yaml, Profiles.class);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
    this.separator = ',';
  }

  public Profiles getProfiles() {
    return profiles;
  }

  public SchemaMetadata create() throws MolgenisException {
    List<Row> keepRows = new ArrayList<>();

    for (String schemaLoc : profiles.modelsList) {
      SchemaMetadata metadata =
          Emx2.fromRowList(
              CsvTableReader.read(
                  new InputStreamReader(
                      AbstractDataLoader.class.getClassLoader().getResourceAsStream(schemaLoc))));

      for (Row row : Emx2.toRowList(metadata)) {
        List<String> profiles = csvStringToList(row.getString("profiles"));

        if (profiles.isEmpty()) {
          throw new MolgenisException("No profiles for " + row);
        }

        boolean profileFound = false;
        for (String profile : profiles) {
          if (this.profiles.profilesList.contains(profile)) {
            keepRows.add(row);
            profileFound = true;
            break;
          }
        }
        if (!profileFound) {
          System.out.println("discard: " + row);
        }
      }
    }

    SchemaMetadata generatedSchema = Emx2.fromRowList(keepRows);

    return generatedSchema;
  }

  public static void main(String args[]) throws IOException {
    SchemaFromProfile sfp = new SchemaFromProfile("fairdatahub/FAIRDataHub.yaml");
    SchemaMetadata generatedSchema = sfp.create();
    System.out.println("resulting schema: " + generatedSchema);
  }
}

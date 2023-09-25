package org.molgenis.emx2.datamodels.profiles;

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
import org.molgenis.emx2.io.readers.CsvTableWriter;

public class SchemaCsvFromProfile {

  private String yamlFileLocation;
  private String outputDatamodelCsv;
  private Profiles profiles;
  private Character separator;

  public SchemaCsvFromProfile(String yamlFileLocation) {
    if (!yamlFileLocation.endsWith(".yaml")) {
      throw new MolgenisException("Input YAML file name must end in '.yaml'");
    }
    this.yamlFileLocation = yamlFileLocation;
    this.outputDatamodelCsv =
        this.yamlFileLocation.substring(0, this.yamlFileLocation.length() - 5)
            + "_generatedFromProfile.csv";
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

  public String generate() throws MolgenisException {
    List<Row> keepRows = new ArrayList<>();

    for (String schemaLoc : profiles.modelsList) {
      SchemaMetadata metadata =
          Emx2.fromRowList(
              CsvTableReader.read(
                  new InputStreamReader(
                      AbstractDataLoader.class.getClassLoader().getResourceAsStream(schemaLoc))));

      for (Row row : Emx2.toRowList(metadata)) {
        String profiles = row.getString("profiles");
        if (profiles == null) {
          throw new MolgenisException("Missing profiles for " + row);
        }
        if (this.profiles.profilesList.contains(profiles)) {
          keepRows.add(row);
        }
      }
    }

    try {
      FileWriter writer = new FileWriter("outputDatamodelCsv");
      Writer bufferedWriter = new BufferedWriter(writer);
      if (keepRows.iterator().hasNext()) {
        CsvTableWriter.write(
            keepRows,
            keepRows.get(0).getColumnNames().stream().toList(),
            bufferedWriter,
            separator);
      }
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
    // System.out.println(row);
    //  System.out.println(row.getString("columnName") + " " + row.getString("profiles"));

    // System.out.println("profile loaded:" + profiles.toString());
    return this.outputDatamodelCsv;
  }

  public static void main(String args[]) throws IOException {
    SchemaCsvFromProfile sfp = new SchemaCsvFromProfile("fairdatahub/FAIRDataHub.yaml");
    String outFile = sfp.generate();
    System.out.println("output at " + outFile);
  }
}

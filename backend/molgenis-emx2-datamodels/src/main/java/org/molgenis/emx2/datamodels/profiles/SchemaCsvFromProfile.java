package org.molgenis.emx2.datamodels.profiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.AbstractDataLoader;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class SchemaCsvFromProfile {

  private String yamlFileLocation;
  private String outputDatamodelCsv;

  public SchemaCsvFromProfile(String yamlFileLocation) {
    if (!yamlFileLocation.endsWith(".yaml")) {
      throw new MolgenisException("Input YAML file name must end in '.yaml'");
    }
    this.yamlFileLocation = yamlFileLocation;
    this.outputDatamodelCsv =
        this.yamlFileLocation.substring(0, this.yamlFileLocation.length() - 5)
            + "_generatedFromProfile.csv";
  }

  public String generate() throws IOException {
    InputStreamReader yaml =
        new InputStreamReader(
            Objects.requireNonNull(
                AbstractDataLoader.class
                    .getClassLoader()
                    .getResourceAsStream(this.yamlFileLocation)));
    Profiles profiles = new ObjectMapper(new YAMLFactory()).readValue(yaml, Profiles.class);

    for (String schemaLoc : profiles.datamodelsList) {
      SchemaMetadata metadata =
          Emx2.fromRowList(
              CsvTableReader.read(
                  new InputStreamReader(
                      AbstractDataLoader.class.getClassLoader().getResourceAsStream(schemaLoc))));
      System.out.println(metadata.toString());
    }

    System.out.println("profile loaded:" + profiles.toString());
    return this.outputDatamodelCsv;
  }

  public static void main(String args[]) throws IOException {
    SchemaCsvFromProfile sfp = new SchemaCsvFromProfile("fairdatahub/FAIRDataHub.yaml");
    String outFile = sfp.generate();
    System.out.println("output at " + outFile);
  }
}

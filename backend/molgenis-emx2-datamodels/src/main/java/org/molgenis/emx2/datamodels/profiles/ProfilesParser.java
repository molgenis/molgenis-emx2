package org.molgenis.emx2.datamodels.profiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import org.molgenis.emx2.datamodels.AbstractDataLoader;

public class ProfilesParser {

  public static void main(String args[]) throws IOException {
    InputStreamReader yaml =
        new InputStreamReader(
            Objects.requireNonNull(
                AbstractDataLoader.class
                    .getClassLoader()
                    .getResourceAsStream("fairdatahub/FAIRDataHub.yaml")));

    Profiles profiles = new ObjectMapper(new YAMLFactory()).readValue(yaml, Profiles.class);
    System.out.println("profile loaded:" + profiles.toString());
  }
}

package org.molgenis.emx2.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.yaml.format.YamlPackageDescription;

public class TestMolgenisYaml {

  @Test
  public void testCatalogue() throws IOException {
    DataModels.Profile catalogueCatalogue = DataModels.Profile.DATA_CATALOGUE;
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile(catalogueCatalogue.getTemplate());
    SchemaMetadata schema = schemaFromProfile.create();

    YamlFactory yamlFactory = new YamlFactory();
    Map<String, String> bundle =
        yamlFactory.toYamlBundle(
            YamlPackageDescription.builder()
                .id("catalogue_and_registry")
                .description("just a test package")
                .version("8.0.0")
                .build(),
            schema,
            YamlFactory.Options.builder()
                .packageName("catalogue_and_registry")
                .tableAsImports(true)
                .build());

    assertEquals(37, bundle.size());

    for (Map.Entry<String, String> entry : bundle.entrySet()) {
      System.out.println("#" + entry.getKey());
      System.out.println(entry.getValue());
      System.out.println("---");
    }

    System.out.println(bundle.keySet());

    writeToTestResources("catalogue_and_registry", bundle);
  }

  public static void writeToTestResources(String packageName, Map<String, String> files)
      throws IOException {
    Path baseDir = Paths.get("src", "test", "resources", packageName);

    for (Map.Entry<String, String> entry : files.entrySet()) {
      Path target = baseDir.resolve(entry.getKey()).normalize();

      // Create parent directories
      Files.createDirectories(target.getParent());

      // Write file
      Files.writeString(
          target,
          entry.getValue(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    }
  }
}

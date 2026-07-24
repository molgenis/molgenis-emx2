package org.molgenis.emx2.datamodels.profiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Emx2CorpusYamlConverterTest {

  @Test
  void multipleDefinitionRowsAreSkippedLoudly(@TempDir Path root) throws IOException {
    Path models = Files.createDirectories(root.resolve("models").resolve("shared"));
    Files.writeString(
        models.resolve("multidef.csv"),
        "tableName,columnName,columnType,key\n" + "Foo,,,\n" + "Foo,id,string,1\n" + "Foo,,,\n");
    Path output = root.resolve("yaml");

    new Emx2CorpusYamlConverter().run(root.resolve("models"), output);

    String manifest = Files.readString(output.resolve("SKIPPED.md"));
    assertTrue(manifest.contains("shared/multidef.csv"), manifest);
    assertTrue(manifest.contains("multiple definition rows for table(s): Foo"), manifest);
    assertTrue(
        Files.notExists(output.resolve("shared").resolve("multidef.yaml")),
        "a lossily-mapping model must not be converted");
  }
}

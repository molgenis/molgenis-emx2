package org.molgenis.emx2.io.yaml2;

import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class TestYaml2SchemaLoader {

  @Test
  void testLoadSchema() throws IOException {
    Yaml2Loader loader = new Yaml2Loader();

    URL resource = getClass().getClassLoader().getResource("yaml2/PetStore.yaml");
    Schema schema = loader.loadSchema(resource);

    System.out.println(loader.toYaml(schema));
  }

  @Test
  void testLoadEntity() throws IOException {
    Yaml2Loader loader = new Yaml2Loader();

    URL resource = getClass().getClassLoader().getResource("yaml2/petstore/Pets.yaml");
    Entity entity = loader.loadEntity(resource);

    System.out.println(loader.toYaml(entity));
  }
}

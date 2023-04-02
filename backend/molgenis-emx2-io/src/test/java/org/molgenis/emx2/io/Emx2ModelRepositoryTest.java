package org.molgenis.emx2.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import org.junit.Test;
import org.molgenis.emx2.io.yaml.ModelRepository;

public class Emx2ModelRepositoryTest {

  @Test
  public void loadSchemaFile() throws JsonProcessingException {

    ClassLoader classLoader = getClass().getClassLoader();
    File archetypesDir = new File(classLoader.getResource("archetypes").getFile());
    File instancesDir = new File(classLoader.getResource("instances").getFile());
    ModelRepository modelRepository = new ModelRepository(archetypesDir, instancesDir);

    // simple roundtrip for now
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    System.out.println(mapper.writeValueAsString(modelRepository));
  }
}

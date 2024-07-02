package org.molgenis.emx2.web.transformers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.molgenis.emx2.MolgenisException;

public class ActionTransformer {

  ObjectMapper objectMapper = new ObjectMapper();

  public <T> T transform(String input, Class<T> valueType) {
    try {
      return objectMapper.readValue(input, valueType);
    } catch (JsonProcessingException e) {
      throw new MolgenisException("Could not parse JSON", e);
    }
  }
}

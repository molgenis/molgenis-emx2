package org.molgenis.emx2.web.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonTransformer {

  private final ObjectMapper jacksonMapper = new ObjectMapper();

  public String render(Object model) throws Exception {
    return jacksonMapper.writeValueAsString(model);
  }
}

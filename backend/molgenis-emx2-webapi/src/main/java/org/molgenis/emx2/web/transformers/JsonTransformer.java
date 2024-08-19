package org.molgenis.emx2.web.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {

  private final ObjectMapper jacksonMapper = new ObjectMapper();

  @Override
  public String render(Object model) throws Exception {
    return jacksonMapper.writeValueAsString(model);
  }
}

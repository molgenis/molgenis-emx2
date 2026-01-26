package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsltTransformEngine {
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JsonNode transform(Path jsltTemplatePath, JsonNode input) throws IOException {
    if (!Files.exists(jsltTemplatePath)) {
      throw new IOException("JSLT template not found: " + jsltTemplatePath);
    }

    String jsltTemplate = Files.readString(jsltTemplatePath);
    // preserve empty arrays/objects in output
    Expression jslt = new Parser(new StringReader(jsltTemplate)).withObjectFilter("true").compile();
    return jslt.apply(input);
  }

  public JsonNode loadJson(Path jsonPath) throws IOException {
    if (!Files.exists(jsonPath)) {
      throw new IOException("JSON file not found: " + jsonPath);
    }
    return objectMapper.readTree(jsonPath.toFile());
  }
}

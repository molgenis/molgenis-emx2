package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import com.schibsted.spt.data.jslt.ResourceResolver;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsltTransformEngine {
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JsonNode transform(Path jsltTemplatePath, JsonNode input) throws IOException {
    if (!Files.exists(jsltTemplatePath)) {
      throw new IOException("JSLT template not found: " + jsltTemplatePath);
    }

    Path baseDir = jsltTemplatePath.getParent();
    String jsltTemplate = Files.readString(jsltTemplatePath);

    ResourceResolver resolver = new FileResourceResolver(baseDir);
    Expression jslt =
        new Parser(new StringReader(jsltTemplate))
            .withResourceResolver(resolver)
            .withObjectFilter("true")
            .compile();
    return jslt.apply(input);
  }

  public JsonNode loadJson(Path jsonPath) throws IOException {
    if (!Files.exists(jsonPath)) {
      throw new IOException("JSON file not found: " + jsonPath);
    }
    return objectMapper.readTree(jsonPath.toFile());
  }

  private static class FileResourceResolver implements ResourceResolver {
    private final Path baseDir;

    FileResourceResolver(Path baseDir) {
      this.baseDir = baseDir;
    }

    @Override
    public Reader resolve(String path) {
      try {
        Path resolved = PathValidator.validateWithinBase(baseDir, path);
        return Files.newBufferedReader(resolved);
      } catch (IOException e) {
        throw new RuntimeException("Cannot load resource '" + path + "': not found", e);
      }
    }
  }
}

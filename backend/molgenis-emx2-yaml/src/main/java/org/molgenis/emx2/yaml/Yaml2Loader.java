package org.molgenis.emx2.yaml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.molgenis.emx2.MolgenisException;

public class Yaml2Loader {
  static ObjectMapper mapper =
      new ObjectMapper(
              new YAMLFactory()
                  .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                  .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

  public Schema loadSchema(URL url) {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      Schema schema = mapper.readValue(url, Schema.class);
      schema.setSourceURL(url);
      schema.loadImports();
      return schema;
    } catch (IOException e) {
      throw new MolgenisException("Cannot load schema from path: " + url + ". " + e.getMessage());
    }
  }

  public static Entity loadEntity(URL url) {
    try {
      Entity entity = mapper.readValue(url, Entity.class);
      entity.setSourceURL(url);
      entity.loadImports();
      return entity;
    } catch (IOException e) {
      throw new MolgenisException("Cannot load entity from path: " + url + ". " + e.getMessage());
    }
  }

  public static Field loadField(URL url) {
    try {
      Field field = mapper.readValue(url, Field.class);
      field.setSourceURL(url);
      return field;
    } catch (IOException e) {
      throw new MolgenisException("Cannot load field from path: " + url + ". " + e.getMessage());
    }
  }

  public String toYaml(Schema schema) throws JsonProcessingException {
    return mapper.writeValueAsString(schema);
  }

  public String toYaml(Entity entity) throws JsonProcessingException {
    return mapper.writeValueAsString(entity);
  }

  public static URL resolveImportUrl(String path, URL baseURL) {
    // 1. Try interpreting it as a full URL
    try {
      return new URL(path);
    } catch (MalformedURLException ignore) {
    }

    // 2. Resolve relative to base URL (if given)
    if (baseURL != null) {
      try {
        // Combine base + relative path
        URL resolved = new URL(baseURL, path);
        if (new File(resolved.toURI()).exists()) {
          return resolved;
        }
      } catch (Exception ignore) {
        // Fallback to next method
      }

      // 3. Try with .yaml extension relative to base
      try {
        URL resolvedWithYaml = new URL(baseURL, path + ".yaml");
        if (new File(resolvedWithYaml.toURI()).exists()) {
          return resolvedWithYaml;
        }
      } catch (Exception ignore) {
        // Fallback to next method
      }
    }

    // 4. Try as local file path (absolute or relative to current working dir)
    Path filePath = Paths.get(path).toAbsolutePath().normalize();
    if (filePath.toFile().exists()) {
      try {
        return filePath.toUri().toURL();
      } catch (MalformedURLException e) {
        throw new MolgenisException("Invalid file path: " + filePath, e);
      }
    }

    // 5. Try with .yaml added
    Path yamlPath = Paths.get(path + ".yaml").toAbsolutePath().normalize();
    if (yamlPath.toFile().exists()) {
      try {
        return yamlPath.toUri().toURL();
      } catch (MalformedURLException e) {
        throw new MolgenisException("Invalid file path: " + yamlPath, e);
      }
    }

    // Still not found
    throw new MolgenisException(
        "Could not resolve import URL '" + path + "' using baseURL " + baseURL);
  }

  public static URL getBaseUrl(URL url) {
    if (url == null) return null;
    String urlString = url.toString();
    int lastSlash = urlString.lastIndexOf('/');
    if (lastSlash == -1) {
      throw new MolgenisException("URL does not contain a path separator: " + url);
    }
    try {
      return new URL(urlString.substring(0, lastSlash + 1)); // include trailing slash
    } catch (Exception e) {
      throw new MolgenisException("Failed to extract base URL from: " + url, e);
    }
  }

  public static class SingleValueListSerializer extends JsonSerializer<List<String>> {
    @Override
    public void serialize(List<String> value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      if (value != null && value.size() == 1) {
        gen.writeString(value.get(0));
      } else {
        gen.writeStartArray();
        for (String s : value) {
          gen.writeString(s);
        }
        gen.writeEndArray();
      }
    }
  }
}

package org.molgenis.emx2.rdf.jsonld;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonLdUtils {

  private static final int MAX_DEPTH = 100;
  private static final ObjectMapper mapper =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  private static final Logger logger = LoggerFactory.getLogger(JsonLdUtils.class);

  private JsonLdUtils() {}

  public static String convertToTurtle(
      Map<String, Object> jsonLdSchema, Map<String, Object> graphqlLikeData) throws IOException {
    Map wrapper = new LinkedHashMap<>();
    wrapper.putAll(jsonLdSchema);
    wrapper.put("data", graphqlLikeData);
    try (StringReader reader = new StringReader(mapper.writeValueAsString(wrapper))) {
      RDFParser parser = Rio.createParser(RDFFormat.JSONLD);
      parser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);
      Model model = new LinkedHashModel();
      parser.setRDFHandler(new StatementCollector(model));
      parser.parse(reader, "");
      StringWriter writer = new StringWriter();
      Rio.write(model, writer, RDFFormat.TURTLE);
      return writer.toString();
    } catch (Exception e) {
      validateJsonLd(wrapper);
      throw new MolgenisException("Convert to turtle failed", e);
    }
  }

  public static Map<String, Object> stripJsonLdKeywords(Map<String, Object> data) {
    if (data == null) {
      return null;
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      String key = entry.getKey();
      if (!key.startsWith("@")) {
        Object value = entry.getValue();
        if (value instanceof Map) {
          result.put(key, stripJsonLdKeywords((Map<String, Object>) value));
        } else if (value instanceof List) {
          result.put(key, stripJsonLdKeywords((List) value));
        } else {
          result.put(key, value);
        }
      }
    }
    return result;
  }

  private static List stripJsonLdKeywords(List data) {
    if (data == null) {
      return null;
    }
    List result = new ArrayList<>();
    for (Object item : data) {
      if (item instanceof Map) {
        result.add(stripJsonLdKeywords((Map<String, Object>) item));
      } else if (item instanceof List) {
        result.add(stripJsonLdKeywords((List) item));
      } else {
        result.add(item);
      }
    }
    return result;
  }

  public static void validateJsonLd(Map<String, Object> jsonLd) {
    JsonNode rootNode = mapper.valueToTree(jsonLd);
    Map<String, String> centralPrefixes = new LinkedHashMap<>();

    JsonNode topContext = rootNode.get("@context");
    if (topContext != null) {
      extractPrefixes(topContext, centralPrefixes, "@context.", 0);
      checkContextTypes(topContext, "@context.", 0);
    }

    JsonNode graph = rootNode.get("data");
    if (graph != null) {
      scanNode(graph, centralPrefixes, "data.", 0);
    }
  }

  private static void scanNode(
      JsonNode node, Map<String, String> inheritedPrefixes, String nodePath, int depth) {
    if (depth > MAX_DEPTH) {
      throw new MolgenisException("JSON-LD structure too deeply nested (max 100 levels)");
    }

    Map<String, String> localPrefixes = new LinkedHashMap<>(inheritedPrefixes);

    if (node.isObject()) {
      JsonNode localContext = node.get("@context");

      if (localContext != null) {
        extractPrefixes(localContext, localPrefixes, nodePath + "@context.", depth + 1);
        checkContextTypes(localContext, nodePath + "@context.", depth + 1);
      }

      JsonNode typeVal = node.get("@type");
      if (typeVal != null) {
        checkTypeOrId(typeVal, localPrefixes, nodePath + "@type");
      }

      JsonNode idVal = node.get("@id");
      if (idVal != null) {
        checkTypeOrId(idVal, localPrefixes, nodePath + "@id");
      }

      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        if (!"@context".equals(entry.getKey())) {
          scanNode(entry.getValue(), localPrefixes, nodePath + entry.getKey() + ".", depth + 1);
        }
      }
    } else if (node.isArray()) {
      int i = 0;
      for (JsonNode item : node) {
        scanNode(item, localPrefixes, nodePath + "[" + i + "].", depth + 1);
        i++;
      }
    }
  }

  private static void extractPrefixes(
      JsonNode ctxObj, Map<String, String> prefixes, String path, int depth) {
    if (depth > MAX_DEPTH) {
      throw new MolgenisException("JSON-LD structure too deeply nested (max 100 levels)");
    }

    if (ctxObj.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = ctxObj.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String key = entry.getKey();
        JsonNode val = entry.getValue();

        if (val.isTextual()) {
          prefixes.put(key, val.asText());
        } else if (val.isObject()) {
          JsonNode idVal = val.get("@id");
          if (idVal != null && idVal.isTextual()) {
            checkTypeOrId(idVal, prefixes, path + key + ".@id");
          }
          extractPrefixes(val, prefixes, path + key + ".", depth + 1);
        } else if (val.isArray()) {
          int i = 0;
          for (JsonNode item : val) {
            extractPrefixes(item, prefixes, path + key + "[" + i + "]", depth + 1);
            i++;
          }
        }
      }
    } else if (ctxObj.isArray()) {
      int i = 0;
      for (JsonNode item : ctxObj) {
        extractPrefixes(item, prefixes, path + "[" + i + "]", depth + 1);
        i++;
      }
    }
  }

  private static void checkContextTypes(JsonNode ctxObj, String path, int depth) {
    if (depth > MAX_DEPTH) {
      throw new MolgenisException("JSON-LD structure too deeply nested (max 100 levels)");
    }

    if (ctxObj.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = ctxObj.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String key = entry.getKey();
        JsonNode val = entry.getValue();

        if (val.isObject()) {
          JsonNode typeVal = val.get("@type");
          if (typeVal != null && !typeVal.isTextual()) {
            throw new MolgenisException(
                "Invalid @type in @context at path "
                    + path
                    + key
                    + ": must be a string, found "
                    + typeVal.getNodeType());
          }
          checkContextTypes(val, path + key + ".", depth + 1);
        } else if (val.isArray()) {
          int i = 0;
          for (JsonNode item : val) {
            checkContextTypes(item, path + key + "[" + i + "].", depth + 1);
            i++;
          }
        }
      }
    } else if (ctxObj.isArray()) {
      int i = 0;
      for (JsonNode item : ctxObj) {
        checkContextTypes(item, path + "[" + i + "]", depth + 1);
        i++;
      }
    }
  }

  private static void checkTypeOrId(JsonNode val, Map<String, String> prefixes, String path) {
    if (val.isTextual()) {
      checkPrefixedIri(val.asText(), prefixes, path);
    } else if (val.isArray()) {
      int i = 0;
      for (JsonNode item : val) {
        if (item.isTextual()) {
          checkPrefixedIri(item.asText(), prefixes, path + "[" + i + "]");
        }
        i++;
      }
    } else {
      throw new MolgenisException(
          "Invalid value at path "
              + path
              + ": must be string or array, found "
              + val.getNodeType());
    }
  }

  private static void checkPrefixedIri(String value, Map<String, String> prefixes, String path) {
    if (value == null || value.isEmpty()) {
      throw new MolgenisException("Invalid empty @id/@type at path: " + path);
    }
    if (value.startsWith("_:") || value.startsWith("http")) return;
    if (value.contains(":")) {
      String prefix = value.substring(0, value.indexOf(':'));
      if (!prefixes.containsKey(prefix)) {
        throw new MolgenisException(
            "Missing prefix '" + prefix + "' at path: " + path + " -> " + value);
      }
    } else {
      throw new MolgenisException("Invalid IRI at path: " + path + " -> " + value);
    }
  }
}

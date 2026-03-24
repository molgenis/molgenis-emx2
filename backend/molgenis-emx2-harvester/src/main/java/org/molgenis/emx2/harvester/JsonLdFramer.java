package org.molgenis.emx2.harvester;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.rdf4j.model.Model;

public class JsonLdFramer {
  private static final String JSON_LD_CONTEXT = "@context";
  private final RdfToJsonLd converter;
  private final ObjectMapper objectMapper;

  public JsonLdFramer() {
    this.converter = new RdfToJsonLd();
    this.objectMapper = new ObjectMapper();
  }

  public JsonNode frame(Model model, JsonNode frame) throws IOException {
    try {
      String expandedJsonLd = converter.convert(model);
      JsonDocument expandedDoc = JsonDocument.of(new StringReader(expandedJsonLd));
      JsonNode framingFrame = buildFramingFrame(frame);
      String frameStr = objectMapper.writeValueAsString(framingFrame);
      JsonStructure frameStructure;
      try (JsonReader frameReader = Json.createReader(new StringReader(frameStr))) {
        frameStructure = frameReader.read();
      }
      JsonDocument frameDoc = JsonDocument.of(frameStructure);
      JsonLdOptions options = new JsonLdOptions();
      options.setOmitGraph(true);
      JsonStructure framedJson = JsonLd.frame(expandedDoc, frameDoc).options(options).get();
      String framedStr = framedJson.toString();
      JsonNode framedResult = objectMapper.readTree(framedStr);
      if (framedResult.isObject() && frame.has(JSON_LD_CONTEXT)) {
        ObjectNode result = objectMapper.createObjectNode();
        result.set(JSON_LD_CONTEXT, frame.get(JSON_LD_CONTEXT));
        framedResult
            .fields()
            .forEachRemaining(entry -> result.set(entry.getKey(), entry.getValue()));
        return result;
      }
      return framedResult;
    } catch (Exception e) {
      throw new IOException("Failed to frame JSON-LD: " + e.getMessage(), e);
    }
  }

  private JsonNode buildFramingFrame(JsonNode frame) {
    ObjectNode framingFrame = objectMapper.createObjectNode();
    JsonNode originalContext = frame.get(JSON_LD_CONTEXT);
    if (originalContext != null && originalContext.isObject()) {
      ObjectNode strippedContext = objectMapper.createObjectNode();
      Iterator<Map.Entry<String, JsonNode>> fields = originalContext.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        if (entry.getValue().isTextual()) {
          strippedContext.set(entry.getKey(), entry.getValue());
        }
      }
      framingFrame.set(JSON_LD_CONTEXT, strippedContext);
    }
    JsonNode embedValue = frame.get("@embed");
    if (embedValue != null) {
      framingFrame.set("@embed", embedValue);
    }
    return framingFrame;
  }
}

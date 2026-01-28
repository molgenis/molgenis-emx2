package org.molgenis.emx2.fairmapper.rdf;

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
import org.eclipse.rdf4j.model.Model;

public class JsonLdFramer {
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

      String frameStr = objectMapper.writeValueAsString(frame);

      JsonReader jsonReader = Json.createReader(new StringReader(frameStr));
      JsonStructure frameStructure = jsonReader.read();
      JsonDocument frameDoc = JsonDocument.of(frameStructure);

      JsonLdOptions options = new JsonLdOptions();
      options.setOmitGraph(true);

      JsonStructure framedJson = JsonLd.frame(expandedDoc, frameDoc).options(options).get();

      String framedStr = framedJson.toString();
      JsonNode framedResult = objectMapper.readTree(framedStr);

      if (framedResult.isObject() && frame.has("@context")) {
        ObjectNode result = objectMapper.createObjectNode();
        result.set("@context", frame.get("@context"));
        framedResult.fields().forEachRemaining(e -> result.set(e.getKey(), e.getValue()));
        return result;
      }

      return framedResult;

    } catch (Exception e) {
      throw new IOException("Failed to frame JSON-LD: " + e.getMessage(), e);
    }
  }
}

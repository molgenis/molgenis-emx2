package org.molgenis.emx2.fairmapper.rdf;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
      JsonDocument frameDoc = JsonDocument.of(new StringReader(frameStr));

      JsonStructure framedJson = JsonLd.frame(expandedDoc, frameDoc).get();

      String framedStr = framedJson.toString();
      return objectMapper.readTree(framedStr);

    } catch (Exception e) {
      throw new IOException("Failed to frame JSON-LD: " + e.getMessage(), e);
    }
  }
}

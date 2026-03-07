package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FrameAnalyzerTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final FrameAnalyzer analyzer = new FrameAnalyzer();

  @Test
  void extractsEmbeddedPredicates() throws Exception {
    JsonNode frame =
        MAPPER.readTree(
            """
            {
              "@context": {"dcat": "http://www.w3.org/ns/dcat#", "dcterms": "http://purl.org/dc/terms/", "foaf": "http://xmlns.com/foaf/0.1/"},
              "@type": "dcat:Catalog",
              "dcterms:publisher": {"@embed": "@always", "foaf:name": {}},
              "dcat:dataset": {"@type": "dcat:Dataset", "@embed": "@always"}
            }
            """);
    Map<Integer, List<String>> result = analyzer.analyze(frame, 2);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    List<String> depth0 = result.get(0);
    assertNotNull(depth0);
    assertTrue(depth0.contains("dcterms:publisher"));
    assertTrue(depth0.contains("dcat:dataset"));
  }

  @Test
  void skipsJsonLdKeywords() throws Exception {
    JsonNode frame = MAPPER.readTree("{\"@context\":{}, \"@type\":\"dcat:Catalog\"}");
    Map<Integer, List<String>> result = analyzer.analyze(frame, 1);
    assertTrue(result.isEmpty());
  }

  @Test
  void respectsMaxDepth() throws Exception {
    JsonNode frame =
        MAPPER.readTree(
            "{\"outer\": {\"@embed\": \"@always\", \"inner\": {\"@embed\": \"@always\"}}}");
    Map<Integer, List<String>> depthZeroOnly = analyzer.analyze(frame, 0);
    assertEquals(1, depthZeroOnly.size());
    assertTrue(depthZeroOnly.containsKey(0));
    assertFalse(depthZeroOnly.containsKey(1));
  }

  @Test
  void ignoresNonEmbedObjects() throws Exception {
    JsonNode frame = MAPPER.readTree("{\"dcterms:title\": {}}");
    Map<Integer, List<String>> result = analyzer.analyze(frame, 2);
    assertTrue(result.isEmpty());
  }
}

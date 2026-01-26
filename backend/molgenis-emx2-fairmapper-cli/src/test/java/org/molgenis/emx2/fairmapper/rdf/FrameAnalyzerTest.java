package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FrameAnalyzerTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  void testAnalyzeSimpleFrame() throws Exception {
    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#",
            "dcterms": "http://purl.org/dc/terms/"
          },
          "@type": "dcat:Catalog",
          "dcterms:title": {},
          "dcat:dataset": {
            "@type": "dcat:Dataset",
            "@embed": "@always",
            "dcterms:title": {}
          }
        }
        """;

    com.fasterxml.jackson.databind.JsonNode frame = mapper.readTree(frameJson);
    FrameAnalyzer analyzer = new FrameAnalyzer();

    Map<Integer, List<String>> result = analyzer.analyze(frame, 2);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.containsKey(0));
    assertEquals(1, result.get(0).size());
    assertEquals("dcat:dataset", result.get(0).get(0));
  }

  @Test
  void testAnalyzeNestedFrame() throws Exception {
    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#",
            "dcterms": "http://purl.org/dc/terms/"
          },
          "@type": "dcat:Catalog",
          "dcat:dataset": {
            "@type": "dcat:Dataset",
            "@embed": "@always",
            "dcterms:title": {},
            "dcat:distribution": {
              "@type": "dcat:Distribution",
              "@embed": "@always",
              "dcterms:title": {}
            }
          }
        }
        """;

    com.fasterxml.jackson.databind.JsonNode frame = mapper.readTree(frameJson);
    FrameAnalyzer analyzer = new FrameAnalyzer();

    Map<Integer, List<String>> result = analyzer.analyze(frame, 2);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.containsKey(0));
    assertTrue(result.containsKey(1));
    assertEquals("dcat:dataset", result.get(0).get(0));
    assertEquals("dcat:distribution", result.get(1).get(0));
  }

  @Test
  void testAnalyzeWithoutEmbed() throws Exception {
    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#",
            "dcterms": "http://purl.org/dc/terms/"
          },
          "@type": "dcat:Catalog",
          "dcterms:title": {},
          "dcat:dataset": {
            "@type": "dcat:Dataset",
            "dcterms:title": {}
          }
        }
        """;

    com.fasterxml.jackson.databind.JsonNode frame = mapper.readTree(frameJson);
    FrameAnalyzer analyzer = new FrameAnalyzer();

    Map<Integer, List<String>> result = analyzer.analyze(frame, 2);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testAnalyzeRespectMaxDepth() throws Exception {
    String frameJson =
        """
        {
          "@type": "dcat:Catalog",
          "dcat:dataset": {
            "@embed": "@always",
            "dcat:distribution": {
              "@embed": "@always",
              "dcat:nested": {
                "@embed": "@always"
              }
            }
          }
        }
        """;

    com.fasterxml.jackson.databind.JsonNode frame = mapper.readTree(frameJson);
    FrameAnalyzer analyzer = new FrameAnalyzer();

    Map<Integer, List<String>> result = analyzer.analyze(frame, 1);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.containsKey(0));
    assertTrue(result.containsKey(1));
    assertFalse(result.containsKey(2));
  }
}

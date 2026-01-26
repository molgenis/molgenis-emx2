package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.FairMapperException;

class FrameDrivenFetcherTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  private Model parseTurtle(String turtle) throws IOException {
    return Rio.parse(new StringReader(turtle), "", RDFFormat.TURTLE);
  }

  @Test
  void testFetchWithoutLinkFollowing() throws Exception {
    String catalogTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/catalog/1> a dcat:Catalog ;
            dcterms:title "Test Catalog" ;
            dcterms:description "A test catalog" .
        """;

    Model catalogModel = parseTurtle(catalogTurtle);

    RdfSource mockSource =
        url -> {
          if ("http://example.org/catalog/1".equals(url)) {
            return catalogModel;
          }
          throw new IOException("Not found: " + url);
        };

    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#",
            "dcterms": "http://purl.org/dc/terms/"
          },
          "@type": "dcat:Catalog",
          "dcterms:title": {}
        }
        """;

    JsonNode frame = mapper.readTree(frameJson);
    FrameDrivenFetcher fetcher = new FrameDrivenFetcher(mockSource, new FrameAnalyzer());

    Model result = fetcher.fetch("http://example.org/catalog/1", frame, 1);

    assertEquals(catalogModel.size(), result.size());
  }

  @Test
  void testFetchFollowsLinks() throws Exception {
    String catalogTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/catalog/1> a dcat:Catalog ;
            dcterms:title "Test Catalog" ;
            dcat:dataset <http://example.org/dataset/1> ,
                         <http://example.org/dataset/2> .
        """;

    String dataset1Turtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/dataset/1> a dcat:Dataset ;
            dcterms:title "Dataset One" ;
            dcterms:identifier "doi:10.1234/one" .
        """;

    String dataset2Turtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/dataset/2> a dcat:Dataset ;
            dcterms:title "Dataset Two" ;
            dcterms:identifier "doi:10.1234/two" .
        """;

    Map<String, Model> models = new HashMap<>();
    models.put("http://example.org/catalog/1", parseTurtle(catalogTurtle));
    models.put("http://example.org/dataset/1", parseTurtle(dataset1Turtle));
    models.put("http://example.org/dataset/2", parseTurtle(dataset2Turtle));

    RdfSource mockSource =
        url -> {
          Model model = models.get(url);
          if (model == null) {
            throw new IOException("Not found: " + url);
          }
          return model;
        };

    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#",
            "dcterms": "http://purl.org/dc/terms/"
          },
          "@type": "dcat:Catalog",
          "dcterms:title": {},
          "http://www.w3.org/ns/dcat#dataset": {
            "@type": "dcat:Dataset",
            "@embed": "@always",
            "dcterms:title": {},
            "dcterms:identifier": {}
          }
        }
        """;

    JsonNode frame = mapper.readTree(frameJson);
    FrameDrivenFetcher fetcher = new FrameDrivenFetcher(mockSource, new FrameAnalyzer());

    Model result = fetcher.fetch("http://example.org/catalog/1", frame, 1);

    int expectedSize =
        models.get("http://example.org/catalog/1").size()
            + models.get("http://example.org/dataset/1").size()
            + models.get("http://example.org/dataset/2").size();

    assertEquals(expectedSize, result.size());
  }

  @Test
  void testFetchWithNestedLinks() throws Exception {
    String catalogTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/catalog/1> a dcat:Catalog ;
            dcterms:title "Test Catalog" ;
            dcat:dataset <http://example.org/dataset/1> .
        """;

    String datasetTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/dataset/1> a dcat:Dataset ;
            dcterms:title "Dataset One" ;
            dcat:distribution <http://example.org/distribution/1> .
        """;

    String distributionTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/distribution/1> a dcat:Distribution ;
            dcterms:title "CSV Distribution" ;
            dcat:downloadURL <http://example.org/data.csv> .
        """;

    Map<String, Model> models = new HashMap<>();
    models.put("http://example.org/catalog/1", parseTurtle(catalogTurtle));
    models.put("http://example.org/dataset/1", parseTurtle(datasetTurtle));
    models.put("http://example.org/distribution/1", parseTurtle(distributionTurtle));

    RdfSource mockSource =
        url -> {
          Model model = models.get(url);
          if (model == null) {
            throw new IOException("Not found: " + url);
          }
          return model;
        };

    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#",
            "dcterms": "http://purl.org/dc/terms/"
          },
          "@type": "dcat:Catalog",
          "http://www.w3.org/ns/dcat#dataset": {
            "@type": "dcat:Dataset",
            "@embed": "@always",
            "http://www.w3.org/ns/dcat#distribution": {
              "@type": "dcat:Distribution",
              "@embed": "@always"
            }
          }
        }
        """;

    JsonNode frame = mapper.readTree(frameJson);
    FrameDrivenFetcher fetcher = new FrameDrivenFetcher(mockSource, new FrameAnalyzer());

    Model result = fetcher.fetch("http://example.org/catalog/1", frame, 2);

    int expectedSize =
        models.get("http://example.org/catalog/1").size()
            + models.get("http://example.org/dataset/1").size()
            + models.get("http://example.org/distribution/1").size();

    assertEquals(expectedSize, result.size());
  }

  @Test
  void testFetchHandlesFailedLinks() throws Exception {
    String catalogTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/catalog/1> a dcat:Catalog ;
            dcterms:title "Test Catalog" ;
            dcat:dataset <http://example.org/dataset/missing> .
        """;

    Model catalogModel = parseTurtle(catalogTurtle);

    RdfSource mockSource =
        url -> {
          if ("http://example.org/catalog/1".equals(url)) {
            return catalogModel;
          }
          throw new IOException("Not found: " + url);
        };

    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#"
          },
          "@type": "dcat:Catalog",
          "http://www.w3.org/ns/dcat#dataset": {
            "@embed": "@always"
          }
        }
        """;

    JsonNode frame = mapper.readTree(frameJson);
    FrameDrivenFetcher fetcher = new FrameDrivenFetcher(mockSource, new FrameAnalyzer());

    Model result = fetcher.fetch("http://example.org/catalog/1", frame, 1);

    assertEquals(catalogModel.size(), result.size());
  }

  @Test
  void testFrameAndOutput() throws Exception {
    String catalogTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/catalog/1> a dcat:Catalog ;
            dcterms:title "Test Catalog" ;
            dcat:dataset <http://example.org/dataset/1> .
        """;

    String datasetTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/dataset/1> a dcat:Dataset ;
            dcterms:title "Dataset One" .
        """;

    Map<String, Model> models = new HashMap<>();
    models.put("http://example.org/catalog/1", parseTurtle(catalogTurtle));
    models.put("http://example.org/dataset/1", parseTurtle(datasetTurtle));

    RdfSource mockSource =
        url -> {
          Model model = models.get(url);
          if (model == null) {
            throw new IOException("Not found: " + url);
          }
          return model;
        };

    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#",
            "dcterms": "http://purl.org/dc/terms/"
          },
          "@type": "dcat:Catalog",
          "dcterms:title": {},
          "http://www.w3.org/ns/dcat#dataset": {
            "@type": "dcat:Dataset",
            "@embed": "@always",
            "dcterms:title": {}
          }
        }
        """;

    JsonNode frame = mapper.readTree(frameJson);
    FrameDrivenFetcher fetcher = new FrameDrivenFetcher(mockSource, new FrameAnalyzer());

    Model model = fetcher.fetch("http://example.org/catalog/1", frame, 1);

    JsonLdFramer framer = new JsonLdFramer();
    JsonNode framedResult = framer.frame(model, frame);

    assertNotNull(framedResult);
    assertTrue(framedResult.has("@graph") || framedResult.has("dcterms:title"));
  }

  @Test
  void testErrorBehaviorWarnAndContinue() throws Exception {
    String catalogTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/catalog/1> a dcat:Catalog ;
            dcterms:title "Test Catalog" ;
            dcat:dataset <http://example.org/dataset/missing> .
        """;

    Model catalogModel = parseTurtle(catalogTurtle);

    RdfSource mockSource =
        url -> {
          if ("http://example.org/catalog/1".equals(url)) {
            return catalogModel;
          }
          throw new IOException("Not found: " + url);
        };

    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#"
          },
          "@type": "dcat:Catalog",
          "http://www.w3.org/ns/dcat#dataset": {
            "@embed": "@always"
          }
        }
        """;

    JsonNode frame = mapper.readTree(frameJson);
    FrameDrivenFetcher fetcher =
        new FrameDrivenFetcher(
            mockSource, new FrameAnalyzer(), FetchErrorBehavior.WARN_AND_CONTINUE);

    Model result = fetcher.fetch("http://example.org/catalog/1", frame, 1);

    assertEquals(catalogModel.size(), result.size());
  }

  @Test
  void testErrorBehaviorFailFast() throws Exception {
    String catalogTurtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <http://example.org/catalog/1> a dcat:Catalog ;
            dcterms:title "Test Catalog" ;
            dcat:dataset <http://example.org/dataset/missing> .
        """;

    Model catalogModel = parseTurtle(catalogTurtle);

    RdfSource mockSource =
        url -> {
          if ("http://example.org/catalog/1".equals(url)) {
            return catalogModel;
          }
          throw new IOException("Not found: " + url);
        };

    String frameJson =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#"
          },
          "@type": "dcat:Catalog",
          "http://www.w3.org/ns/dcat#dataset": {
            "@embed": "@always"
          }
        }
        """;

    JsonNode frame = mapper.readTree(frameJson);
    FrameDrivenFetcher fetcher =
        new FrameDrivenFetcher(mockSource, new FrameAnalyzer(), FetchErrorBehavior.FAIL_FAST);

    FairMapperException exception =
        assertThrows(
            FairMapperException.class,
            () -> fetcher.fetch("http://example.org/catalog/1", frame, 1));

    assertTrue(exception.getMessage().contains("Failed to fetch"));
    assertTrue(exception.getMessage().contains("http://example.org/dataset/missing"));
    assertNotNull(exception.getCause());
    assertTrue(exception.getCause() instanceof IOException);
  }
}

package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.FairMapperException;
import org.molgenis.emx2.fairmapper.UrlValidator;

class RdfFetcherTest {

  private static final String SAMPLE_TURTLE =
      """
      @prefix dcat: <http://www.w3.org/ns/dcat#> .
      @prefix dcterms: <http://purl.org/dc/terms/> .

      <https://example.org/catalog/123>
        a dcat:Catalog ;
        dcterms:title "Test Catalog" ;
        dcterms:description "A test catalog for unit testing" .
      """;

  @Test
  void testParseTurtle() throws IOException {
    RdfFetcher fetcher = new RdfFetcher("https://example.org/catalog");

    Model model = fetcher.parseTurtle(SAMPLE_TURTLE);

    assertNotNull(model);
    assertEquals(3, model.size());
  }

  @Test
  void testInvalidTurtle() {
    RdfFetcher fetcher = new RdfFetcher("https://example.org/catalog");

    IOException exception =
        assertThrows(IOException.class, () -> fetcher.parseTurtle("invalid turtle content"));

    assertTrue(exception.getMessage().contains("Failed to parse Turtle RDF"));
  }

  @Test
  void testFetchFromRealFdp() throws IOException {
    String fdpUrl = "https://fdp.radboudumc.nl/catalog/d7522c39-a774-496f-998a-fdeb262a5c65";
    RdfFetcher fetcher = new RdfFetcher(fdpUrl);

    Model model = fetcher.fetch(fdpUrl);

    assertNotNull(model);
    assertTrue(model.size() > 0, "Expected statements in the model");

    System.out.println("Fetched " + model.size() + " statements from real FDP");
  }

  @Test
  void testConvertToJsonLd() throws IOException {
    RdfFetcher fetcher = new RdfFetcher("https://example.org/catalog");
    RdfToJsonLd converter = new RdfToJsonLd();

    Model model = fetcher.parseTurtle(SAMPLE_TURTLE);
    String jsonLd = converter.convert(model);

    assertNotNull(jsonLd);
    assertTrue(jsonLd.contains("\"@id\""));
    assertTrue(jsonLd.contains("https://example.org/catalog/123"));

    System.out.println("Generated JSON-LD:");
    System.out.println(jsonLd);
  }

  @Test
  void testSmallResponseWithinLimit() throws IOException {
    UrlValidator validator = new UrlValidator("https://example.org/catalog");
    RdfFetcher fetcher = new RdfFetcher(validator, 1000);

    Model model = fetcher.parseTurtle(SAMPLE_TURTLE);

    assertNotNull(model);
    assertEquals(3, model.size());
  }

  @Test
  void testResponseExceedingMaxBytes() {
    UrlValidator validator = new UrlValidator("https://example.org/catalog");
    RdfFetcher fetcher = new RdfFetcher(validator, 10);

    FairMapperException exception =
        assertThrows(FairMapperException.class, () -> fetcher.parseTurtle(SAMPLE_TURTLE));

    assertTrue(exception.getMessage().contains("Response body too large"));
    assertTrue(exception.getMessage().contains("max: 10"));
  }

  @Test
  void testRetryOnTransient500Error() throws IOException, InterruptedException {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(500);
    when(mockResponse.body()).thenReturn("Internal Server Error");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    RdfFetcher fetcher = createFetcherWithMockClient(mockClient);

    IOException exception =
        assertThrows(IOException.class, () -> fetcher.fetch("https://example.org/catalog"));

    assertTrue(exception.getMessage().contains("status 500"));
    verify(mockClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  void testNoRetryOn404Error() throws IOException, InterruptedException {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(404);
    when(mockResponse.body()).thenReturn("Not Found");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    RdfFetcher fetcher = createFetcherWithMockClient(mockClient);

    IOException exception =
        assertThrows(IOException.class, () -> fetcher.fetch("https://example.org/catalog"));

    assertTrue(exception.getMessage().contains("status 404"));
    verify(mockClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  void testSuccessOnFirstAttempt() throws IOException, InterruptedException {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);

    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(SAMPLE_TURTLE);
    when(mockResponse.headers()).thenReturn(mock(java.net.http.HttpHeaders.class));
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    RdfFetcher fetcher = createFetcherWithMockClient(mockClient);

    Model model = fetcher.fetch("https://example.org/catalog");

    assertNotNull(model);
    assertEquals(3, model.size());
    verify(mockClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  private RdfFetcher createFetcherWithMockClient(HttpClient mockClient) {
    return new RdfFetcher(new UrlValidator("https://example.org/catalog")) {
      @Override
      protected HttpClient createHttpClient() {
        return mockClient;
      }
    };
  }
}

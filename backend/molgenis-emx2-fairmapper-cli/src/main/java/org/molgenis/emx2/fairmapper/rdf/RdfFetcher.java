package org.molgenis.emx2.fairmapper.rdf;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.fairmapper.FairMapperException;
import org.molgenis.emx2.fairmapper.UrlValidator;

public class RdfFetcher implements RdfSource {
  private static final long DEFAULT_MAX_BYTES = 10 * 1024 * 1024;
  private final HttpClient httpClient;
  private final UrlValidator urlValidator;
  private final long maxBytes;

  public RdfFetcher(String sourceUrl) {
    this(sourceUrl, false);
  }

  public RdfFetcher(String sourceUrl, boolean allowExternal) {
    this(new UrlValidator(sourceUrl, allowExternal));
  }

  public RdfFetcher(UrlValidator urlValidator) {
    this(urlValidator, DEFAULT_MAX_BYTES);
  }

  public RdfFetcher(UrlValidator urlValidator, long maxBytes) {
    this.urlValidator = urlValidator;
    this.maxBytes = maxBytes;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  @Override
  public Model fetch(String url) throws IOException {
    urlValidator.validate(url);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "text/turtle")
            .timeout(Duration.ofSeconds(60))
            .GET()
            .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new IOException(
            "RDF fetch failed with status " + response.statusCode() + ": " + response.body());
      }

      response.headers().firstValueAsLong("Content-Length").ifPresent(this::validateSize);

      String body = response.body();
      return parseTurtle(body);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("RDF fetch interrupted", e);
    }
  }

  public Model parseTurtle(String turtleContent) throws IOException {
    validateSize(turtleContent.length());
    try {
      return Rio.parse(new StringReader(turtleContent), "", RDFFormat.TURTLE);
    } catch (RDFParseException e) {
      throw new IOException("Failed to parse Turtle RDF: " + e.getMessage(), e);
    }
  }

  private void validateSize(long size) {
    if (size > maxBytes) {
      throw new FairMapperException(
          "Response body too large: " + size + " bytes (max: " + maxBytes + ")");
    }
  }
}

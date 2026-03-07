package org.molgenis.emx2.fairmapper.rdf;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.fairmapper.dcat.DcatHarvestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfFetcher implements RdfSource {
  private static final Logger log = LoggerFactory.getLogger(RdfFetcher.class);
  private static final long DEFAULT_MAX_BYTES = 10 * 1024 * 1024;
  private static final int MAX_RETRIES = 3;
  private static final int BASE_DELAY_MS = 1000;
  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
  private final HttpClient httpClient;
  private final long maxBytes;

  public RdfFetcher() {
    this(DEFAULT_MAX_BYTES);
  }

  public RdfFetcher(long maxBytes) {
    this.maxBytes = maxBytes;
    this.httpClient = createHttpClient();
  }

  protected HttpClient createHttpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  private void validateUrl(String urlString) {
    URI uri;
    try {
      uri = URI.create(urlString);
    } catch (IllegalArgumentException e) {
      throw new DcatHarvestException("Invalid URL: " + urlString);
    }
    String scheme = uri.getScheme();
    if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
      throw new DcatHarvestException(
          "Invalid URL scheme: " + scheme + ". Allowed: " + ALLOWED_SCHEMES);
    }
  }

  @Override
  public Model fetch(String url) throws IOException {
    validateUrl(url);
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        return doFetch(url);
      } catch (IOException e) {
        if (!isTransientError(e) || attempt == MAX_RETRIES) {
          throw e;
        }
        int delayMs = BASE_DELAY_MS * (1 << (attempt - 1));
        log.warn(
            "Fetch attempt {} failed for {}: {}, retrying in {}ms...",
            attempt,
            url,
            e.getMessage(),
            delayMs);
        sleep(delayMs);
      }
    }
    throw new IOException("Unreachable");
  }

  private Model doFetch(String url) throws IOException {
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

  private boolean isTransientError(IOException e) {
    String msg = e.getMessage();
    if (msg == null) return false;
    return msg.contains("status 5")
        || msg.contains("timed out")
        || msg.contains("Connection reset")
        || msg.contains("Connection refused");
  }

  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new DcatHarvestException("Retry interrupted", e);
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
      throw new DcatHarvestException(
          "Response body too large: " + size + " bytes (max: " + maxBytes + ")");
    }
  }
}

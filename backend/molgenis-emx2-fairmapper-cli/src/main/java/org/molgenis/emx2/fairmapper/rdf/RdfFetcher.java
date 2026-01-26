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

public class RdfFetcher implements RdfSource {
  private final HttpClient httpClient;

  public RdfFetcher() {
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  @Override
  public Model fetch(String url) throws IOException {
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

      return parseTurtle(response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("RDF fetch interrupted", e);
    }
  }

  public Model parseTurtle(String turtleContent) throws IOException {
    try {
      return Rio.parse(new StringReader(turtleContent), "", RDFFormat.TURTLE);
    } catch (RDFParseException e) {
      throw new IOException("Failed to parse Turtle RDF: " + e.getMessage(), e);
    }
  }
}

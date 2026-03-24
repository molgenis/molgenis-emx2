package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;

public class Extractor {

  private static final HttpClient CLIENT =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(30))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  public Model extract(String url) throws IOException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "text/turtle")
            .timeout(Duration.ofSeconds(60))
            .GET()
            .build();

    try {
      HttpResponse<InputStream> response =
          CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() != 200) {
        throw new IOException(
            "RDF fetch failed with status " + response.statusCode() + ": " + response.body());
      }

      Files.write(Path.of("pet-store.ttl"), response.body().readAllBytes());
      return parseTurtle(response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("RDF fetch interrupted", e);
    }
  }

  private static Model parseTurtle(InputStream stream) throws IOException {
    try {
      return Rio.parse(stream, RDFFormat.TURTLE);
    } catch (RDFParseException e) {
      throw new IOException("Failed to parse Turtle RDF: " + e.getMessage(), e);
    }
  }
}

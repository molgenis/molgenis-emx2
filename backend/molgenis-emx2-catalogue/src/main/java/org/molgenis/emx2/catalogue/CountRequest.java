package org.molgenis.emx2.catalogue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountRequest {
  private static final Logger logger = LoggerFactory.getLogger(CountRequest.class);

  public HttpResponse send() {
    String endPoint = "https://data-catalogue.molgeniscloud.org/catalogue/catalogue/graphql";

    String query = """
            { "query": "{  Cohorts_agg {  count  } }"}
    """;

    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(endPoint))
              .headers("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(query))
              .build();

      return HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (URISyntaxException | IOException | InterruptedException e) {
      e.printStackTrace();
      logger.error("failed to do count request");
    }
    return null;
  }
}

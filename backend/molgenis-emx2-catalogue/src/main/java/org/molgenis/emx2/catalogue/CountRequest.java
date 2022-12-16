package org.molgenis.emx2.catalogue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountRequest {
  private static final Logger logger = LoggerFactory.getLogger(CountRequest.class);

  static class UncheckedObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper {
    /** Parses the given JSON string into a Map. */
    Map<String, Object> readValue(String content) {
      try {
        return this.readValue(
            content.replace("\n", "").replace("\r", ""), new TypeReference<>() {});
      } catch (IOException ioe) {
        throw new CompletionException(ioe);
      }
    }
  }

  public JsonNode send() throws URISyntaxException, IOException, InterruptedException {
    String endPoint = "https://data-catalogue.molgeniscloud.org/catalogue/catalogue/graphql";
    UncheckedObjectMapper objectMapper = new UncheckedObjectMapper();

    String query = """
            { "query": "{  Cohorts_agg {  count  } }"}
    """;

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(new URI(endPoint))
            .headers("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(query))
            .build();
    HttpClient client = HttpClient.newBuilder().build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    Map<String, Object> value = objectMapper.readValue(response.body());
    return objectMapper.readTree(objectMapper.writeValueAsString(value));
  }
}

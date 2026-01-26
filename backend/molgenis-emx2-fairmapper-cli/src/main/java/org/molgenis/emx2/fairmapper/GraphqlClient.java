package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GraphqlClient {
  private final HttpClient httpClient;
  private final String baseUrl;
  private final String token;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public GraphqlClient(String baseUrl, String token) {
    this.baseUrl = normalizeBaseUrl(baseUrl);
    this.token = token;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  private String normalizeBaseUrl(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  public JsonNode execute(String schema, String query, JsonNode variables) throws IOException {
    String url = baseUrl + "/" + schema + "/api/graphql";

    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("query", query);
    if (variables != null && variables.isObject() && variables.size() > 0) {
      requestBody.set("variables", variables);
    }

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(
                HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));

    if (token != null && !token.isBlank()) {
      requestBuilder.header("x-molgenis-token", token);
    }

    HttpRequest request = requestBuilder.build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new IOException(
            "GraphQL request failed with status " + response.statusCode() + ": " + response.body());
      }

      JsonNode result = objectMapper.readTree(response.body());

      if (result.has("errors")
          && result.get("errors").isArray()
          && result.get("errors").size() > 0) {
        throw new IOException("GraphQL errors: " + result.get("errors").toPrettyString());
      }

      return result;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("GraphQL request interrupted", e);
    }
  }
}

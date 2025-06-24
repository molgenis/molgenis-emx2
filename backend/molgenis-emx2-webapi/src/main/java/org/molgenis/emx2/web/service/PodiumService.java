package org.molgenis.emx2.web.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class PodiumService {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public PodiumService(HttpClient httpClient) {
    this.httpClient = httpClient;
    this.objectMapper = new ObjectMapper();
  }

  public HttpResponse<String> getResponse(Context context)
      throws InterruptedException, IOException {
    String body = context.body();
    PodiumRequest podiumRequest = objectMapper.readValue(body, PodiumRequest.class);
    String authHeader =
        getBasicAuthenticationHeader(podiumRequest.podiumUsername, podiumRequest.podiumPassword);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(podiumRequest.podiumUrl))
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(podiumRequest.payload)))
            .header("Authorization", authHeader)
            .header("Content-type", "application/json")
            .build();

    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static String getBasicAuthenticationHeader(String username, String password) {
    String valueToEncode = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
  }

  public static class PodiumRequest {
    @JsonProperty public String podiumUrl;
    @JsonProperty public String podiumUsername;
    @JsonProperty public String podiumPassword;
    @JsonProperty public Object payload;

    public PodiumRequest() {
      // exposed for test
    }
  }
}

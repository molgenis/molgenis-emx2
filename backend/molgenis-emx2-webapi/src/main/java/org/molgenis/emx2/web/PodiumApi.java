package org.molgenis.emx2.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.jetbrains.annotations.NotNull;

public class PodiumApi {
  private PodiumApi() {}

  public static void create(Javalin app) {
    final String reportPath = "/api/podium";
    app.post(reportPath, PodiumApi::handlePodiumRequest);
  }

  private static void handlePodiumRequest(@NotNull Context context)
      throws IOException, InterruptedException {
    ObjectMapper objectMapper = new ObjectMapper();
    String body = context.body();
    PodiumRequest podiumRequest = objectMapper.readValue(body, PodiumRequest.class);
    HttpClient client = HttpClient.newHttpClient();

    String basicAuthenticationHeader =
        getBasicAuthenticationHeader(podiumRequest.podiumUsername, podiumRequest.podiumPassword);
    HttpRequest.BodyPublisher bodyPublisher =
        HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(podiumRequest.payload));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(podiumRequest.podiumUrl))
            .POST(bodyPublisher)
            .header("Authorization", basicAuthenticationHeader)
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 202) {
      context.status(201);
      context.header("Location", response.headers().map().get("Location").getFirst());
    } else {
      context.status(response.statusCode());
    }

    context.result(response.body());
  }

  private static String getBasicAuthenticationHeader(String username, String password) {
    String valueToEncode = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
  }

  private static class PodiumRequest {
    @JsonProperty public String podiumUrl;
    @JsonProperty public String podiumUsername;
    @JsonProperty public String podiumPassword;
    @JsonProperty public Object payload;
  }
}

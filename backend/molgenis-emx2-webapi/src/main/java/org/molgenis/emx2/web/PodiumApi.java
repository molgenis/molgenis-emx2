package org.molgenis.emx2.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class PodiumApi {

  private PodiumApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    final String podiumPath = "/api/podium";
    app.post(podiumPath, PodiumApi::handlePodiumRequest);
  }

  public static void handlePodiumRequest(Context ctx) throws IOException, URISyntaxException {
    String body = ctx.body();
    ObjectMapper mapper = new ObjectMapper();
    PodiumBody podiumBody = mapper.readValue(body, PodiumBody.class);

    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String payload = ow.writeValueAsString(podiumBody.payload);

    HttpRequest request = HttpRequest.newBuilder()
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .uri(new URI(podiumBody.podiumUrl))
        .header("Authorization", getBasicAuthenticationHeader(podiumBody.podiumUsername, podiumBody.podiumPassword))
        .build();

  }

  private static String getBasicAuthenticationHeader(String username, String password) {
    String valueToEncode = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
  }

  private static class PodiumBody {
    @JsonProperty("podiumUrl")
    public String podiumUrl;

    @JsonProperty("podiumUsername")
    public String podiumUsername;

    @JsonProperty("podiumPassword")
    public String podiumPassword;

    @JsonProperty("payload")
    public Object payload;
  }

  private static class PodiumPayload {
    @JsonProperty("URL")
    public String URL;

    @JsonProperty("humanReadable")
    public String humanReadable;

    @JsonProperty("collections")
    public Object[] collections;
  }
}

package org.molgenis.emx2.fairmapper.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.ContentType;
import io.javalin.http.Header;
import io.javalin.http.HttpStatus;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.web.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Logger logger = LoggerFactory.getLogger(GraphqlClient.class);
  private static final String ERRORS_FIELD = "errors";
  private static final String DATA_FIELD = "data";

  private final String token;
  private final URI endpoint;

  public GraphqlClient(String endpoint, String token) {
    this.token = token;
    this.endpoint = URI.create(endpoint);
  }

  public JsonNode sendQuery(String query) {
    return sendQuery(query, "/api/graphql");
  }

  public JsonNode sendSchemaQuery(String schemaName, String query) {
    return sendQuery(query, "/" + schemaName + "/graphql");
  }

  private JsonNode sendQuery(String query, String path) {
    try (HttpClient client = HttpClient.newHttpClient()) {
      logger.debug("Sending query: {}", query);
      String payload = MAPPER.writeValueAsString(Map.of("query", query));

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(endpoint.resolve(path))
              .header(Header.CONTENT_TYPE, String.valueOf(ContentType.APPLICATION_JSON))
              .header(Constants.X_MOLGENIS_TOKEN, token)
              .POST(HttpRequest.BodyPublishers.ofString(payload))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      logger.debug("Received response: {}", response);

      if (response.statusCode() != HttpStatus.OK.getCode()) {
        handleError(response);
      }

      JsonNode jsonNode = MAPPER.readTree(response.body());
      if (jsonNode.has(ERRORS_FIELD)) {
        throwErrors(jsonNode);
      }

      if (!jsonNode.has(DATA_FIELD)) {
        throw new MolgenisException("Unexpected response from graphql server: " + jsonNode);
      }

      return jsonNode.get(DATA_FIELD);
    } catch (IOException e) {
      throw new MolgenisException("Failed to execute graphql query", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MolgenisException("Interrupted exception occurred when querying", e);
    }
  }

  private void handleError(HttpResponse<String> response) {
    try {
      JsonNode jsonNode = MAPPER.readTree(response.body());
      if (!jsonNode.has(ERRORS_FIELD)) {
        throw new MolgenisException("Unexpected response from graphql server: " + jsonNode);
      }

      throwErrors(jsonNode);
    } catch (JsonProcessingException e) {
      throw new MolgenisException(
          "Unable to read error message from response: " + response.body(), e);
    }
  }

  private static void throwErrors(JsonNode jsonNode) {
    String errors =
        jsonNode
            .get(ERRORS_FIELD)
            .valueStream()
            .map(json -> json.get("message").toString())
            .collect(Collectors.joining(", "));

    throw new MolgenisException(errors);
  }
}

package org.molgenis.emx2.web;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.web.Constants.ACCEPT_FORM_URL_ENC;
import static org.molgenis.emx2.web.Constants.CONTENT_TYPE;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTerm;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTermsResponse;
import org.molgenis.emx2.cafevariome.CafeVariomeQuery;
import org.molgenis.emx2.cafevariome.QueryRecord;
import org.molgenis.emx2.cafevariome.response.RecordIndexResponse;
import org.molgenis.emx2.cafevariome.response.RecordResponse;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CafeVariomeApi {

  private static MolgenisSessionManager sessionManager;
  private static final Logger logger = LoggerFactory.getLogger(CafeVariomeApi.class);

  private static final String CV_CLIENT_SECRET =
      (String) EnvironmentProperty.getParameter(Constants.CV_CLIENT_SECRET, null, STRING);
  private static final String CV_CLIENT_ID =
      (String) EnvironmentProperty.getParameter(Constants.CV_CLIENT_ID, "MolgenisAuth", STRING);
  private static final String CV_INTROSPECT_URI =
      (String)
          EnvironmentProperty.getParameter(
              Constants.CV_INTROSPECT_URI,
              "https://auth1.molgenis.net/realms/Cafe-Variome/protocol/openid-connect/token/introspect",
              STRING);

  public static void create(Javalin app, MolgenisSessionManager sm) {
    sessionManager = sm;
    app.before("/{schema}/api/cafevariome/record", CafeVariomeApi::checkAuth);
    app.post("/{schema}/api/cafevariome/record", CafeVariomeApi::postRecord);
    app.before("/{schema}/api/cafevariome/record-index", CafeVariomeApi::checkAuth);
    app.get("/{schema}/api/cafevariome/record-index", CafeVariomeApi::getRecordIndex);
  }

  private static void checkAuth(Context ctx) throws IOException, InterruptedException {
    MolgenisSession session = sessionManager.getSession(ctx.req());
    if (!session.getDatabase().isAnonymous()) {
      return;
    }
    if (!hasKeycloakConfiguration()) return;
    HttpResponse<String> response;
    try (HttpClient client = HttpClient.newHttpClient()) {

      Enumeration<String> authHeaders = ctx.req().getHeaders(AUTHORIZATION);
      String authHeader = authHeaders.nextElement();
      String accessToken = authHeader.split(" ")[1];
      String formData =
          "client_id="
              + URLEncoder.encode(CV_CLIENT_ID, StandardCharsets.UTF_8)
              + "&client_secret="
              + URLEncoder.encode(CV_CLIENT_SECRET, StandardCharsets.UTF_8)
              + "&token="
              + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

      HttpRequest request =
          HttpRequest.newBuilder()
              .header(CONTENT_TYPE, ACCEPT_FORM_URL_ENC)
              .uri(URI.create(CV_INTROSPECT_URI))
              .POST(HttpRequest.BodyPublishers.ofString(formData))
              .build();
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    if (response.statusCode() != 200) {
      ctx.status(response.statusCode());
    }
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonResponse = objectMapper.readTree(response.body());
    String user = String.valueOf(jsonResponse.get("email"));

    Database database = session.getDatabase();
    if (!database.hasUser(user)) {
      logger.info("Add new user({}) to database", user);
      database.addUser(user);
    }
    database.setActiveUser(user);
    logger.info("Sign in for user: {}", user);
  }

  private static boolean hasKeycloakConfiguration() {
    return CV_CLIENT_SECRET != null && CV_CLIENT_ID != null && CV_INTROSPECT_URI != null;
  }

  private static void postRecord(Context ctx) throws JsonProcessingException {
    ctx.json(QueryRecord.post(getSchema(ctx), ctx.body()));
  }

  private static void getRecordIndex(Context ctx) {
    Schema schema = getSchema(ctx);

    Database database = sessionManager.getSession(ctx.req()).getDatabase();

    Map<String, String> attributes = new HashMap<>();
    Map<String, String> values = new HashMap<>();
    Map<String, List<String>> mappings = new HashMap<>();

    Set<FilteringTerm> filteringTermsSet =
        new FilteringTermsResponse(database)
            .getFilteringTermsFromTables(
                List.of(EntryType.INDIVIDUALS.getName()), schema.getName());

    for (FilteringTerm filteringTerm : filteringTermsSet) {
      if (filteringTerm.getType().equals("ontology")) {
        String filteringTermName = filteringTerm.getColumn().getName();
        attributes.put(filteringTermName, filteringTermName);
        values.put(filteringTerm.getId(), filteringTerm.getLabel());
        if (mappings.containsKey(filteringTermName)) {
          List<String> terms = mappings.get(filteringTermName);
          terms.add(filteringTerm.getId());
        } else {
          List<String> terms = new ArrayList<>();
          terms.add(filteringTerm.getId());
          mappings.put(filteringTermName, terms);
        }
      } else if (filteringTerm.getType().equals("alphanumeric")) {
        attributes.put(filteringTerm.getId(), filteringTerm.getId());
      }
    }

    RecordResponse records = QueryRecord.post(schema, new CafeVariomeQuery());
    RecordIndexResponse response =
        new RecordIndexResponse(
            records.recordCount(), new RecordIndexResponse.EavIndex(attributes, values, mappings));

    ctx.json(response);
  }
}

package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static spark.Spark.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.endpoints.*;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import spark.Request;
import spark.Response;

public class BeaconApi {

  private static MolgenisSessionManager sessionManager;

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;
    defineRoutes("/api/beacon");
    defineRoutes("/api/beacon_vp");
  }

  private static void defineRoutes(String basePath) {
    path(
        basePath,
        () -> {
          before("/*", BeaconApi::processRequest);
          get("/", BeaconApi::getInfo);
          get("/info", BeaconApi::getInfo);
          get("/service-info", BeaconApi::getInfo);
          get("/configuration", BeaconApi::getConfiguration);
          get("/map", BeaconApi::getMap);
          get("/filtering_terms", BeaconApi::getFilteringTerms);
          get("/entry_types", BeaconApi::getEntryTypes);
          get("/:entry_type", BeaconApi::getEntryType);
          get("/:entry_type/:id", BeaconApi::getEntryType);
          get("/:entry_type_id/:id/:entry_type", BeaconApi::getEntryType);
          post("/:entry_type", BeaconApi::postEntryType);
        });
  }

  private static void processRequest(Request request, Response response) {
    int specificationIndex = 2;
    String specification = request.matchedPath().split("/")[specificationIndex];
    request.attribute("specification", specification);
    response.type(Constants.ACCEPT_JSON);
  }

  private static String getEntryType(Request request, Response response) throws Exception {
    return entryTypeRequest(request, new BeaconRequestBody(request));
  }

  private static String postEntryType(Request request, Response response) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(request.body(), BeaconRequestBody.class);
    beaconRequest.addRequestParameters(request);
    return entryTypeRequest(request, beaconRequest);
  }

  private static String entryTypeRequest(Request request, BeaconRequestBody requestBody)
      throws JsonProcessingException {

    Database database = sessionManager.getSession(request).getDatabase();
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode dataResult = queryEntryType.query(database);
    return getWriter().writeValueAsString(dataResult);
  }

  private static String getInfo(Request request, Response response)
      throws JsonProcessingException, URISyntaxException {
    return getWriter().writeValueAsString(new Info(request));
  }

  private static Object getConfiguration(Request request, Response response)
      throws JsonProcessingException {
    return getWriter().writeValueAsString(new Configuration());
  }

  private static Object getMap(Request request, Response response) throws JsonProcessingException {
    return getWriter().writeValueAsString(new Map(request));
  }

  private static Object getEntryTypes(Request request, Response response)
      throws JsonProcessingException {
    return getWriter().writeValueAsString(new EntryTypes());
  }

  private static String getFilteringTerms(Request request, Response response) throws Exception {
    Database database = sessionManager.getSession(request).getDatabase();
    return getWriter().writeValueAsString(new FilteringTerms(database));
  }
}

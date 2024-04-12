package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static spark.Spark.get;
import static spark.Spark.post;

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
    // framework
    get("/api/beacon", BeaconApi::getInfo);
    get("/api/beacon/", BeaconApi::getInfo);
    get("/api/beacon/info", BeaconApi::getInfo);
    get("/api/beacon/service-info", BeaconApi::getInfo);
    get("/api/beacon/configuration", BeaconApi::getConfiguration);

    get("/api/beacon/map", BeaconApi::getMap);
    get("/api/beacon/filtering_terms", BeaconApi::getFilteringTerms);
    get("/api/beacon/entry_types", BeaconApi::getEntryTypes);

    get("/api/beacon/:entry_type", BeaconApi::getEntryType);
    get("/api/beacon/:entry_type/:id", BeaconApi::getEntryType);
    get("/api/beacon/:entry_type_id/:id/:entry_type", BeaconApi::getEntryType);
    post("/api/beacon/:entry_type", BeaconApi::postEntryType);
  }

  private static String getEntryType(Request request, Response response) throws Exception {
    return entryTypeRequest(request, new BeaconRequestBody());
  }

  private static String postEntryType(Request request, Response response) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(request.body(), BeaconRequestBody.class);
    return entryTypeRequest(request, beaconRequest);
  }

  private static String entryTypeRequest(Request request, BeaconRequestBody requestBody)
      throws JsonProcessingException {
    requestBody.addUrlParameters(request);

    Database database = sessionManager.getSession(request).getDatabase();
    JsonNode dataResult = QueryEntryType.query(database, requestBody);
    return getWriter().writeValueAsString(dataResult);
  }

  private static String getInfo(Request request, Response response)
      throws JsonProcessingException, URISyntaxException {
    response.type(Constants.ACCEPT_JSON);
    return getWriter().writeValueAsString(new Info(request));
  }

  private static Object getConfiguration(Request request, Response response)
      throws JsonProcessingException {
    response.type(Constants.ACCEPT_JSON);
    return getWriter().writeValueAsString(new Configuration());
  }

  private static Object getMap(Request request, Response response) throws JsonProcessingException {
    response.type(Constants.ACCEPT_JSON);
    return getWriter().writeValueAsString(new Map(request));
  }

  private static Object getEntryTypes(Request request, Response response)
      throws JsonProcessingException {
    response.type(Constants.ACCEPT_JSON);
    return getWriter().writeValueAsString(new EntryTypes());
  }

  private static String getFilteringTerms(Request request, Response response) throws Exception {
    response.type(Constants.ACCEPT_JSON);
    Database database = sessionManager.getSession(request).getDatabase();
    return getWriter().writeValueAsString(new FilteringTerms(database));
  }
}

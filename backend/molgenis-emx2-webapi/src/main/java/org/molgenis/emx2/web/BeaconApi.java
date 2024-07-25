package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.endpoints.*;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import spark.Request;
import spark.Response;

public class BeaconApi {

  private static MolgenisSessionManager sessionManager;

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;
    defaultResponseTransformer(o -> getWriter().writeValueAsString(o));
    defineRoutes("/:schema/api/beacon");
    defineRoutes("/:schema/api/beacon_vp");
    defineRoutes("/api/beacon");
    defineRoutes("/api/beacon_vp");
    defaultResponseTransformer(null);
  }

  private static void defineRoutes(String basePath) {
    path(
        basePath,
        () -> {
          before("/*", BeaconApi::processRequest);
          get("", BeaconApi::getInfo);
          get("/", BeaconApi::getInfo);
          get("/info", BeaconApi::getInfo);
          get("/service-info", BeaconApi::getInfo);
          get("/configuration", new Configuration()::getResponse);
          get("/map", new Map()::getResponse);
          get("/entry_types", new EntryTypes()::getResponse);
          get("/filtering_terms", BeaconApi::getFilteringTerms);
          get("/:entry_type", BeaconApi::getEntryType);
          get("/:entry_type/:id", BeaconApi::getEntryType);
          get("/:entry_type_id/:id/:entry_type", BeaconApi::getEntryType);
          post("/:entry_type", BeaconApi::postEntryType);
        });
  }

  private static void processRequest(Request request, Response response) {
    extractSpecification(request);
    response.type(Constants.ACCEPT_JSON);
  }

  private static void extractSpecification(Request request) {
    String specification = request.matchedPath().split("/api/")[1].split("/")[0];
    request.attribute("specification", specification);
  }

  private static JsonNode getEntryType(Request request, Response response) {
    return entryTypeRequest(request, new BeaconRequestBody(request));
  }

  private static JsonNode postEntryType(Request request, Response response) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(request.body(), BeaconRequestBody.class);
    beaconRequest.addRequestParameters(request);
    return entryTypeRequest(request, beaconRequest);
  }

  private static JsonNode entryTypeRequest(Request request, BeaconRequestBody requestBody) {
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);

    Schema schema = getSchema(request);
    if (schema != null) return queryEntryType.query(schema);

    Database database = sessionManager.getSession(request).getDatabase();
    return queryEntryType.query(database);
  }

  private static Object getFilteringTerms(Request request, Response response) {
    Database database = sessionManager.getSession(request).getDatabase();
    return new FilteringTerms(database);
  }

  private static Object getInfo(Request request, Response response) {
    Schema schema = getSchema(request);

    Database database = sessionManager.getSession(request).getDatabase();
    return new Info(database).getResponse(schema);
  }
}

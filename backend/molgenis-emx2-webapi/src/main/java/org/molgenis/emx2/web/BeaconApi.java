package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static spark.Spark.get;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.endpoints.*;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.beaconv2.requests.Filter;
import spark.Request;
import spark.Response;

public class BeaconApi {

  private static MolgenisSessionManager sessionManager;
  private static final String APPLICATION_JSON_MIME_TYPE = "application/json";

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
    post("/api/beacon/:entry_type", BeaconApi::postEntryType);
  }

  private static String getInfo(Request request, Response response)
      throws JsonProcessingException, URISyntaxException {
    response.type(APPLICATION_JSON_MIME_TYPE);
    return getWriter().writeValueAsString(new Info(request));
  }

  private static Object getConfiguration(Request request, Response response)
      throws JsonProcessingException {
    response.type(APPLICATION_JSON_MIME_TYPE);
    return getWriter().writeValueAsString(new Configuration());
  }

  private static Object getMap(Request request, Response response) throws JsonProcessingException {
    response.type(APPLICATION_JSON_MIME_TYPE);
    return getWriter().writeValueAsString(new Map(request));
  }

  private static Object getEntryTypes(Request request, Response response)
      throws JsonProcessingException {
    response.type(APPLICATION_JSON_MIME_TYPE);
    return getWriter().writeValueAsString(new EntryTypes());
  }

  private static String getFilteringTerms(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    Database database = sessionManager.getSession(request).getDatabase();
    return getWriter().writeValueAsString(new FilteringTerms(database));
  }

  private static String getEntryType(Request request, Response response) throws Exception {
    EntryType entryType = EntryType.findByName(request.params("entry_type"));

    response.type(APPLICATION_JSON_MIME_TYPE);
    Database database = sessionManager.getSession(request).getDatabase();

    if (entryType == EntryType.GENOMIC_VARIANT) {
      return getWriter().writeValueAsString(new GenomicVariants(request, database));
    }

    JsonNode jsonNode = QueryEntryType.query(database, entryType);
    return getWriter().writeValueAsString(jsonNode);
  }

  private static String postEntryType(Request request, Response response) throws Exception {
    EntryType entryType = EntryType.findByName(request.params("entry_type"));

    response.type(APPLICATION_JSON_MIME_TYPE);
    BeaconRequestBody beaconRequestBody =
        new ObjectMapper().readValue(request.body(), BeaconRequestBody.class);

    Filter[] filters = beaconRequestBody.getQuery().getFilters();
    Database database = sessionManager.getSession(request).getDatabase();
    JsonNode jsonNode = QueryEntryType.query(database, entryType, filters);

    return getWriter().writeValueAsString(jsonNode);
  }
}

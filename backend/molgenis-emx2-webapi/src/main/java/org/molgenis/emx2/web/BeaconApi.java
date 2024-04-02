package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static org.molgenis.emx2.rdf.RDFUtils.extractHost;
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
import org.molgenis.emx2.beaconv2.responses.BeaconBooleanResponse;
import org.molgenis.emx2.beaconv2.responses.BeaconCountResponse;
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
    EntryType entryType = EntryType.findByName(request.params("entry_type"));
    Database database = sessionManager.getSession(request).getDatabase();

    if (entryType == EntryType.GENOMIC_VARIANT) {
      return getWriter().writeValueAsString(new GenomicVariants(request, database));
    }

    BeaconRequestBody requestBody = new BeaconRequestBody(request.params());
    String host = extractHost(request.url());
    requestBody.getMeta().setHost(host);

    JsonNode jsonNode = QueryEntryType.query(database, entryType, requestBody);
    return determineResponse(requestBody, response, jsonNode);
  }

  private static String postEntryType(Request request, Response response) throws Exception {
    EntryType entryType = EntryType.findByName(request.params("entry_type"));
    BeaconRequestBody beaconRequestBody =
        new ObjectMapper().readValue(request.body(), BeaconRequestBody.class);

    String host = extractHost(request.url());
    beaconRequestBody.getMeta().setHost(host);

    Database database = sessionManager.getSession(request).getDatabase();
    JsonNode dataResult = QueryEntryType.query(database, entryType, beaconRequestBody);
    return determineResponse(beaconRequestBody, response, dataResult);
  }

  private static String determineResponse(
      BeaconRequestBody requestBody, Response response, JsonNode dataResult)
      throws JsonProcessingException {
    response.type(Constants.ACCEPT_JSON);
    return switch (requestBody.getQuery().getRequestedGranularity()) {
      case BOOLEAN -> getWriter().writeValueAsString(new BeaconBooleanResponse());
      case AGGREGATED, COUNT -> getWriter().writeValueAsString(new BeaconCountResponse());
      case RECORD, UNDEFINED -> getWriter().writeValueAsString(dataResult);
    };
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

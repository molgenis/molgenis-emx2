package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static spark.Spark.get;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
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

    get("/api/beacon/g_variants", BeaconApi::getGenomicVariants);
    get("/api/beacon/runs", BeaconApi::getRuns);

    get("/api/beacon/:entry_type", BeaconApi::getEntryType);

    post("/api/beacon/individuals", BeaconApi::postIndividuals);
  }

  private static String getEntryType(Request request, Response response)
      throws JsonProcessingException {

    EntryType entryType = EntryType.findByName(request.params("entry_type"));
    if (entryType == null) {
      throw new MolgenisException("Invalid entry type: %s".formatted(request.params("entry_type")));
    }

    response.type(APPLICATION_JSON_MIME_TYPE);
    Database database = sessionManager.getSession(request).getDatabase();
    JsonNode jsonNode = QueryEntryType.query(database, entryType);

    return getWriter().writeValueAsString(jsonNode);
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

  private static String postIndividuals(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    BeaconRequestBody beaconRequestBody =
        new ObjectMapper().readValue(request.body(), BeaconRequestBody.class);

    Filter[] filters = beaconRequestBody.getQuery().getFilters();
    Database database = sessionManager.getSession(request).getDatabase();
    //    JsonNode jsonNode = QueryEntryType.query(database, "Individuals", filters);
    JsonNode jsonNode = null;

    return getWriter().writeValueAsString(jsonNode);
  }

  private static String getRuns(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    List<Table> tables = getTableFromAllSchemas("Runs", request);
    return getWriter().writeValueAsString(new Runs(request, tables));
  }

  private static String getGenomicVariants(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    List<Table> tables = getTableFromAllSchemas("GenomicVariations", request);
    return getWriter().writeValueAsString(new GenomicVariants(request, tables));
  }

  static List<Table> getTableFromAllSchemas(String tableName, Request request) {
    List<Table> tables = new ArrayList<>();
    Collection<String> schemaNames = MolgenisWebservice.getSchemaNames(request);
    Database database = sessionManager.getSession(request).getDatabase();
    for (String sn : schemaNames) {
      Schema schema = database.getSchema(sn);
      Table t = schema.getTable(tableName);
      if (t != null) {
        tables.add(t);
      }
    }
    return tables;
  }

  private static String postDatasets(Request request, Response response)
      throws JsonProcessingException {
    // should parse body into
    BeaconRequestBody requestBody = null; // todo

    // result should be BeaconBooleanResponse, BeaconCountResponse or BeaconCollectionResponse
    return getWriter().writeValueAsString(null);
  }

  private static Object getDatasetsForTable(Request request, Response response)
      throws JsonProcessingException {

    // result should be BeaconBooleanResponse, BeaconCountResponse or beaconResultsetsResponse
    return getWriter().writeValueAsString(null);
  }

  private static Object postDatasetsForTable(Request request, Response response)
      throws JsonProcessingException {

    // should parse body into
    BeaconRequestBody requestBody = null; // todo

    // result should be BeaconBooleanResponse, BeaconCountResponse or beaconResultsetsResponse
    return getWriter().writeValueAsString(null);
  }
}

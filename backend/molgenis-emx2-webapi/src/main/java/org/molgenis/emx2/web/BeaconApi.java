package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static spark.Spark.get;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.*;
import org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp.EJP_VP_IndividualsQuery;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import spark.Request;
import spark.Response;

// is a beacon on level of database, schema or table?
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
    get("/api/beacon/entry_types", BeaconApi::getEntryTypes);
    get("/api/beacon/datasets", BeaconApi::getDatasets);
    get("/api/beacon/g_variants", BeaconApi::getGenomicVariants);
    get("/api/beacon/analyses", BeaconApi::getAnalyses);
    get("/api/beacon/biosamples", BeaconApi::getBiosamples);
    get("/api/beacon/cohorts", BeaconApi::getCohorts);
    get("/api/beacon/individuals", BeaconApi::getIndividuals);
    get("/api/beacon/runs", BeaconApi::getRuns);
    get("/api/beacon/filtering_terms", BeaconApi::getFilteringTerms);

    post("/api/beacon/individuals", BeaconApi::queryIndividuals_EJP_VP);

    /*
    both GET and POST are used to retrieve data, implement both?
    https://docs.genomebeacons.org/variant-queries/#beacon-sequence-queries
     */

    //    get("/:schema/api/beacon/filtering_terms", BeaconApi::getFilteringTerms);
    //
    //    // datasets model
    //    get("/:schema/api/beacon/datasets", BeaconApi::getDatasets);
    //    get("/:schema/api/beacon/datasets/:table", BeaconApi::getDatasetsForTable);
    //    // these are the interesting queries
    //    post("/:schema/api/beacon/datasets", BeaconApi::postDatasets);
    //    post("/:schema/api/beacon/datasets/:table", BeaconApi::postDatasetsForTable);
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

  private static String getDatasets(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    String skip = request.queryParams("skip");
    String limit = request.queryParams("limit");

    // TODO pass request to response to set limits, offsets etc
    // result should be BeaconBooleanResponse, BeaconCountResponse or BeaconCollectionResponse
    List<Table> tables = getTableFromAllSchemas("Dataset", request);
    return getWriter().writeValueAsString(new Datasets(request, tables));
  }

  private static String getAnalyses(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    List<Table> tables = getTableFromAllSchemas("Analyses", request);
    return getWriter().writeValueAsString(new Analyses(request, tables));
  }

  private static String getBiosamples(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    List<Table> tables = getTableFromAllSchemas("Biosamples", request);
    return getWriter().writeValueAsString(new Biosamples(request, tables));
  }

  private static String getCohorts(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    List<Table> tables = getTableFromAllSchemas("Cohorts", request);
    return getWriter().writeValueAsString(new Cohorts(request, tables));
  }

  private static String getIndividuals(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    List<Table> tables = getTableFromAllSchemas("Individuals", request);
    return getWriter().writeValueAsString(new Individuals(request, tables));
  }

  private static String queryIndividuals_EJP_VP(Request request, Response response)
      throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    List<Table> tables = getTableFromAllSchemas("Individuals", request);
    String responseBody = new EJP_VP_IndividualsQuery(request, response, tables).getPostResponse();
    return responseBody;
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

  static Schema[] getSchemasHavingTable(String tableName, Request request) {
    List<Schema> schemas = new ArrayList<>();
    Collection<String> schemaNames = MolgenisWebservice.getSchemaNames(request);
    for (String sn : schemaNames) {
      Schema schema = sessionManager.getSession(request).getDatabase().getSchema(sn);
      Table t = schema.getTable(tableName);
      if (t != null) {
        schemas.add(schema);
      }
    }
    Schema[] schemaArr = new Schema[schemas.size()];
    schemaArr = schemas.toArray(schemaArr);
    return schemaArr;
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

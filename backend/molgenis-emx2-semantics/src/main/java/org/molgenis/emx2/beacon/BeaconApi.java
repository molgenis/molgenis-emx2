package org.molgenis.emx2.beacon;

import static spark.Spark.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.molgenis.emx2.beacon.common.BeaconInformationalResponseMeta;
import org.molgenis.emx2.beacon.requests.BeaconRequestBody;
import org.molgenis.emx2.beacon.responses.*;
import spark.Request;
import spark.Response;

// is a beacon on level of database, schema or table?
public class BeaconApi {

  public static void create() {
    get("/:schema/beacon/", BeaconApi::getInfo);
    get("/:schema/beacon/info", BeaconApi::getInfo);
    get("/:schema/beacon/service-info", BeaconApi::getServiceInfo);
    get("/:schema/beacon/configuration", BeaconApi::getConfiguration);
    get("/:schema/beacon/map", BeaconApi::getMap);
    get("/:schema/beacon/entry_types", BeaconApi::getEntryTypes);
    get("/:schema/beacon/filtering_terms", BeaconApi::getFilteringTerms);
    get("/:schema/beacon/datasets", BeaconApi::getDatasets);
    get("/:schema/beacon/datasets/:table", BeaconApi::getDatasetsForTable);

    // these are the actual queries
    post("/:schema/beacon/datasets", BeaconApi::postDatasets);
    post("/:schema/beacon/datasets/:table", BeaconApi::postDatasetsForTable);
  }

  private static String getInfo(Request req, Response res) throws JsonProcessingException {

    return new ObjectMapper().writeValueAsString(new BeaconInformationalResponseMeta());
  }

  private static String getServiceInfo(Request request, Response response)
      throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(new BeaconServiceInfoResponse());
  }

  private static Object getConfiguration(Request request, Response response)
      throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(new BeaconConfigurationResponse());
  }

  private static Object getMap(Request request, Response response) throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(new BeaconConfigurationResponse());
  }

  private static Object getEntryTypes(Request request, Response response)
      throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(new BeaconEntryTypesResponse());
  }

  private static String getFilteringTerms(Request request, Response response)
      throws JsonProcessingException {
    String skip = request.queryParams("skip");
    String limit = request.queryParams("limit");
    // TODO handle skip and limit
    return new ObjectMapper().writeValueAsString(new BeaconFilteringTermsResponse());
  }

  private static String getDatasets(Request request, Response response)
      throws JsonProcessingException {
    String skip = request.queryParams("skip");
    String limit = request.queryParams("limit");

    // should be subtyp of BeaconQueryResponse
    return new ObjectMapper().writeValueAsString(new BeaconQueryResponse());
  }

  private static String postDatasets(Request request, Response response)
      throws JsonProcessingException {
    // should parse body into
    BeaconRequestBody requestBody = null; // todo

    return new ObjectMapper().writeValueAsString(new BeaconQueryResponse());
  }

  private static Object getDatasetsForTable(Request request, Response response)
      throws JsonProcessingException {

    return new ObjectMapper().writeValueAsString(new BeaconQueryResponse());
  }

  private static Object postDatasetsForTable(Request request, Response response)
      throws JsonProcessingException {

    // should parse body into
    BeaconRequestBody requestBody = null; // todo

    return new ObjectMapper().writeValueAsString(new BeaconQueryResponse());
  }
}

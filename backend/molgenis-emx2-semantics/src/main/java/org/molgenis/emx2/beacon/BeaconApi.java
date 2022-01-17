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

    // framework
    get("/:schema/api/beacon", BeaconApi::getInfo);
    get("/:schema/api/beacon/info", BeaconApi::getInfo);
    get("/:schema/api/beacon/service-info", BeaconApi::getServiceInfo);
    get("/:schema/api/beacon/configuration", BeaconApi::getConfiguration);
    get("/:schema/api/beacon/map", BeaconApi::getMap);
    get("/:schema/api/beacon/entry_types", BeaconApi::getEntryTypes);
    get("/:schema/api/beacon/filtering_terms", BeaconApi::getFilteringTerms);

    // datasets model
    get("/:schema/api/beacon/datasets", BeaconApi::getDatasets);
    get("/:schema/api/beacon/datasets/:table", BeaconApi::getDatasetsForTable);
    // these are the interesting queries
    post("/:schema/api/beacon/datasets", BeaconApi::postDatasets);
    post("/:schema/api/beacon/datasets/:table", BeaconApi::postDatasetsForTable);
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

    // result should be BeaconBooleanResponse, BeaconCountResponse or BeaconCollectionResponse
    return new ObjectMapper().writeValueAsString(null);
  }

  private static String postDatasets(Request request, Response response)
      throws JsonProcessingException {
    // should parse body into
    BeaconRequestBody requestBody = null; // todo

    // result should be BeaconBooleanResponse, BeaconCountResponse or BeaconCollectionResponse
    return new ObjectMapper().writeValueAsString(null);
  }

  private static Object getDatasetsForTable(Request request, Response response)
      throws JsonProcessingException {

    // result should be BeaconBooleanResponse, BeaconCountResponse or beaconResultsetsResponse
    return new ObjectMapper().writeValueAsString(null);
  }

  private static Object postDatasetsForTable(Request request, Response response)
      throws JsonProcessingException {

    // should parse body into
    BeaconRequestBody requestBody = null; // todo

    // result should be BeaconBooleanResponse, BeaconCountResponse or beaconResultsetsResponse
    return new ObjectMapper().writeValueAsString(null);
  }
}

package org.molgenis.emx2.cafevariome.request.parser;

import static org.molgenis.emx2.cafevariome.request.parameters.RequiredQueryParameters.*;

import org.molgenis.emx2.cafevariome.request.parameters.RequiredQueryParameters;
import org.molgenis.emx2.cafevariome.request.query.RequiredQuery;
import spark.Request;

public class RequiredQueryParser {

  /**
   * @param request
   * @throws Exception
   */
  private static void checkRequiredParameters(Request request) throws Exception {
    for (String reqParam : RequiredQueryParameters.reqParams) {
      if (!request.queryParams().contains(reqParam)) {
        throw new Exception("Request requires parameter: " + reqParam);
      }
    }
  }

  /**
   * @param request
   * @return
   */
  public static RequiredQuery getRequiredQueryFromRequest(Request request) throws Exception {
    checkRequiredParameters(request);
    RequiredQuery requiredQuery = new RequiredQuery();
    requiredQuery.setMetaSubjectVariantVersion(request.queryParams(META_SUBJECT_VARIANT_VERSION));
    requiredQuery.setMetaSubjectEAVVersion(request.queryParams(META_SUBJECT_EAV_VERSION));
    requiredQuery.setMetaPhenotypeVersion(request.queryParams(META_PHENOTYPE_VERSION));
    requiredQuery.setMetaQueryIDVersion(request.queryParams(META_QUERY_ID_VERSION));
    requiredQuery.setMetaApiVersion(request.queryParams(META_API_VERSION));
    requiredQuery.setMetaQueryID(request.queryParams(META_QUERY_ID));
    requiredQuery.setMetaQueryLabel(request.queryParams(META_QUERY_LABEL));
    requiredQuery.setReqExistsVersion(request.queryParams(REQ_EXISTS_VERSION));
    requiredQuery.setReqCountVersion(request.queryParams(REQ_COUNT_VERSION));
    requiredQuery.setNetworkKey(request.queryParams(NETWORK_KEY));
    requiredQuery.setCSRFTokenName(request.queryParams(CSRF_TOKEN_NAME));
    return requiredQuery;
  }
}

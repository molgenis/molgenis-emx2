package org.molgenis.emx2.cafevariome.request.parser;

import static org.molgenis.emx2.cafevariome.request.parameters.RequiredQueryParameters.*;

import java.util.Map;
import org.molgenis.emx2.cafevariome.request.parameters.RequiredQueryParameters;
import org.molgenis.emx2.cafevariome.request.query.RequiredQuery;

public class RequiredQueryParser {

  /**
   * @param request
   * @throws Exception
   */
  private static void checkRequiredParameters(Map<String, String> request) throws Exception {
    for (String reqParam : RequiredQueryParameters.reqParams) {
      if (!request.containsKey(reqParam)) {
        throw new Exception("Request requires parameter: " + reqParam);
      }
    }
  }

  /**
   * @param request
   * @return
   */
  public static RequiredQuery getRequiredQueryFromRequest(Map<String, String> request)
      throws Exception {
    checkRequiredParameters(request);
    RequiredQuery requiredQuery = new RequiredQuery();
    requiredQuery.setMetaSubjectVariantVersion(request.get(META_SUBJECT_VARIANT_VERSION));
    requiredQuery.setMetaSubjectEAVVersion(request.get(META_SUBJECT_EAV_VERSION));
    requiredQuery.setMetaPhenotypeVersion(request.get(META_PHENOTYPE_VERSION));
    requiredQuery.setMetaQueryIDVersion(request.get(META_QUERY_ID_VERSION));
    requiredQuery.setMetaApiVersion(request.get(META_API_VERSION));
    requiredQuery.setMetaQueryID(request.get(META_QUERY_ID));
    requiredQuery.setMetaQueryLabel(request.get(META_QUERY_LABEL));
    requiredQuery.setReqExistsVersion(request.get(REQ_EXISTS_VERSION));
    requiredQuery.setReqCountVersion(request.get(REQ_COUNT_VERSION));
    requiredQuery.setNetworkKey(request.get(NETWORK_KEY));
    requiredQuery.setCSRFTokenName(request.get(CSRF_TOKEN_NAME));
    return requiredQuery;
  }
}

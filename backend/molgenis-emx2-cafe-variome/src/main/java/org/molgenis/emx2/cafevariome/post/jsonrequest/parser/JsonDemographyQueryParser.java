package org.molgenis.emx2.cafevariome.post.jsonrequest.parser;

import static org.molgenis.emx2.cafevariome.post.jsonrequest.parser.JsonHPOQueryParser.stringsInArrayProvided;

import org.molgenis.emx2.cafevariome.post.jsonrequest.JsonQuery;
import org.molgenis.emx2.cafevariome.post.request.query.DemographyQuery;

public class JsonDemographyQueryParser {

  /**
   * Check if request has an ORDO query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasDemographyParams(JsonQuery request) throws Exception {
    int demographyLength = request.getQuery().getComponents().getDemography().length;

    if (demographyLength == 0) {
      return false;
    } else if (demographyLength > 1) {
      throw new Exception("More than 1 demography component query is currently unsupported");
    }

    // placeholder code for more than 1 term
    for (int i = 0; i < demographyLength; i++) {
      boolean A =
          stringsInArrayProvided(request.getQuery().getComponents().getDemography()[i].getMinAge());
      boolean B =
          stringsInArrayProvided(request.getQuery().getComponents().getDemography()[i].getMaxAge());
      boolean C =
          stringsInArrayProvided(
              request.getQuery().getComponents().getDemography()[i].getAffected());
      boolean D =
          stringsInArrayProvided(request.getQuery().getComponents().getDemography()[i].getGender());
      boolean E =
          stringsInArrayProvided(
              request.getQuery().getComponents().getDemography()[i].getFamily_type());

      if (A && B && C && D && E) {
        continue;
      } else if (!A && !B && !C && !D) {
        return false; // a complete term is missing, is that OK? or also error?
      } else {
        throw new Exception(
            "Partial demography query parameters supplied, please check your request!");
      }
    }
    return true;
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static DemographyQuery getDemographyQueryFromRequest(JsonQuery request) throws Exception {
    if (request.getQuery().getComponents().getDemography().length > 1) {
      throw new Exception("More than 1 demography component query is currently unsupported");
    }
    if (request.getQuery().getComponents().getDemography()[0].getMinAge().length > 1) {
      throw new Exception("More than 1 minAge in demography query is currently unsupported");
    }
    if (request.getQuery().getComponents().getDemography()[0].getMaxAge().length > 1) {
      throw new Exception("More than 1 maxAge in demography query is currently unsupported");
    }
    if (request.getQuery().getComponents().getDemography()[0].getAffected().length > 1) {
      throw new Exception("More than 1 affected in demography query is currently unsupported");
    }
    if (request.getQuery().getComponents().getDemography()[0].getGender().length > 1) {
      throw new Exception("More than 1 gender in demography query is currently unsupported");
    }
    if (request.getQuery().getComponents().getDemography()[0].getFamily_type().length > 1) {
      throw new Exception("More than 1 family_type in demography query is currently unsupported");
    }

    int minAge =
        Integer.parseInt(request.getQuery().getComponents().getDemography()[0].getMinAge()[0]);
    int maxAge =
        Integer.parseInt(request.getQuery().getComponents().getDemography()[0].getMaxAge()[0]);
    String affected = request.getQuery().getComponents().getDemography()[0].getAffected()[0];
    String gender = request.getQuery().getComponents().getDemography()[0].getGender()[0];
    String family_type = request.getQuery().getComponents().getDemography()[0].getFamily_type()[0];

    DemographyQuery demographyQuery = new DemographyQuery();
    demographyQuery.setMinAge(minAge);
    demographyQuery.setMaxAge(maxAge);
    demographyQuery.setAffected(affected);
    demographyQuery.setGender(gender);
    demographyQuery.setFamily_type(family_type);

    return demographyQuery;
  }
}

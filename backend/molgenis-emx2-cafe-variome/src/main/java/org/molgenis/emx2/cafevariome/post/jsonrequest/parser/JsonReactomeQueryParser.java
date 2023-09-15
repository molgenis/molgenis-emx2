package org.molgenis.emx2.cafevariome.post.jsonrequest.parser;

import static org.molgenis.emx2.cafevariome.post.jsonrequest.parser.JsonHPOQueryParser.stringProvided;
import static org.molgenis.emx2.cafevariome.post.jsonrequest.parser.JsonHPOQueryParser.stringsInArrayProvided;

import org.molgenis.emx2.cafevariome.post.jsonrequest.JsonQuery;
import org.molgenis.emx2.cafevariome.post.request.query.ReactomeQuery;

public class JsonReactomeQueryParser {

  /**
   * Check if request has an ORDO query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasReactomeParams(JsonQuery request) throws Exception {
    int reactomeLength = request.getQuery().getComponents().getReactome().length;

    if (reactomeLength == 0) {
      return false;
    } else if (reactomeLength > 1) {
      throw new Exception("More than 1 Reactome component query is currently unsupported");
    }

    // placeholder code for more than 1 term
    for (int i = 0; i < reactomeLength; i++) {
      boolean A =
          stringProvided(request.getQuery().getComponents().getReactome()[i].getReactom_id());
      boolean B =
          stringsInArrayProvided(
              request.getQuery().getComponents().getReactome()[i].getProtein_effect());
      boolean C = stringProvided(request.getQuery().getComponents().getReactome()[i].getAf());

      if (A && B && C) {
        continue;
        // a whole term is missing, is that OK? or also error?
      } else if (!A && !B && !C) {
        return false;
      } else {
        throw new Exception(
            "Partial Reactome query parameters supplied, please check your request!");
      }
    }
    return true;
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static ReactomeQuery getReactomeQueryFromRequest(JsonQuery request) throws Exception {
    if (request.getQuery().getComponents().getReactome().length > 1) {
      throw new Exception("More than 1 Reactome component query is currently unsupported");
    }
    String reactome_id = request.getQuery().getComponents().getReactome()[0].getReactom_id();
    String[] proteinEffects =
        request.getQuery().getComponents().getReactome()[0].getProtein_effect();
    int af = Integer.parseInt(request.getQuery().getComponents().getReactome()[0].getAf());

    ReactomeQuery reactomeQuery = new ReactomeQuery();
    // set
    return reactomeQuery;
  }
}

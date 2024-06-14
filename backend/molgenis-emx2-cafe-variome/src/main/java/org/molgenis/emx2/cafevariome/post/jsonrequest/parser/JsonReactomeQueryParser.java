package org.molgenis.emx2.cafevariome.post.jsonrequest.parser;

import static org.molgenis.emx2.cafevariome.post.jsonrequest.parser.JsonHPOQueryParser.stringProvided;

import org.molgenis.emx2.cafevariome.post.jsonrequest.JsonQuery;
import org.molgenis.emx2.cafevariome.post.request.query.GeneReactomeQuery;

public class JsonReactomeQueryParser {

  /**
   * Check if request has an Reactome query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasReactomeParams(JsonQuery request) throws Exception {
    if (request.getQuery().getComponents().getReactome() == null) {
      return false;
    }
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

      // ignoring protein effect and AF for the moment

      if (A) {
        continue;
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static GeneReactomeQuery getReactomeQueryFromRequest(JsonQuery request) throws Exception {
    if (request.getQuery().getComponents().getReactome().length > 1) {
      throw new Exception("More than 1 Reactome component query is currently unsupported");
    }
    String reactome_id = request.getQuery().getComponents().getReactome()[0].getReactom_id();
    GeneReactomeQuery reactomeQuery = new GeneReactomeQuery();
    reactomeQuery.setId(reactome_id);
    return reactomeQuery;
  }
}

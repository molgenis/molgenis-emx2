package org.molgenis.emx2.cafevariome.post.jsonrequest.parser;

import static org.molgenis.emx2.cafevariome.post.jsonrequest.parser.JsonHPOQueryParser.stringProvided;

import org.molgenis.emx2.cafevariome.post.jsonrequest.JsonQuery;
import org.molgenis.emx2.cafevariome.post.request.query.GeneReactomeQuery;

public class JsonGeneQueryParser {

  /**
   * Check if request has an Gene query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasGeneParams(JsonQuery request) throws Exception {
    if (request.getQuery().getComponents().getGene() == null) {
      return false;
    }
    int geneLength = request.getQuery().getComponents().getGene().length;
    if (geneLength == 0) {
      return false;
    }
    for (int i = 0; i < geneLength; i++) {
      boolean A = stringProvided(request.getQuery().getComponents().getGene()[i].getGene_id());

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
  public static GeneReactomeQuery[] getGeneQueryFromRequest(JsonQuery request) throws Exception {
    int geneLength = request.getQuery().getComponents().getGene().length;
    GeneReactomeQuery[] geneQueryArr = new GeneReactomeQuery[geneLength];
    for (int i = 0; i < geneLength; i++) {
      String gene_id = request.getQuery().getComponents().getGene()[i].getGene_id();
      GeneReactomeQuery geneQuery = new GeneReactomeQuery();
      geneQuery.setId(gene_id);
      geneQueryArr[i] = geneQuery;
    }
    return geneQueryArr;
  }
}

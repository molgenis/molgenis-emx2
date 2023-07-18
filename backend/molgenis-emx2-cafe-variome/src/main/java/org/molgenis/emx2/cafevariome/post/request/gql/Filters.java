package org.molgenis.emx2.cafevariome.post.request.gql;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.finalizeFilter;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.cafevariome.post.request.query.HPOQuery;
import org.molgenis.emx2.cafevariome.post.request.query.ORDOQuery;

public class Filters {

  /// "{phenotypicFeatures: { featureType: { ontologyTermURI: {like:"

  public static List<String> makeHPOFilter(HPOQuery hpoQuery) throws Exception {
    List<String> result = new ArrayList<>();
    for (String hpoTerm : hpoQuery.getSearchTerms()) {
      if (hpoTerm.startsWith("HP:")) {
        hpoTerm = hpoTerm.replace("HP:", "HP_");
      } else {
        System.out.println("hpoTerm=" + hpoTerm);
        throw new Exception("Expected input HPO term to start with 'HP:'");
      }
      String filter =
          "{phenotypicFeatures: { featureType: { ontologyTermURI: {like:\"" + hpoTerm + "\"";
      filter = finalizeFilter(filter);
      result.add(filter);
    }
    return result;
  }

  public static List<String> makeORDOFilter(ORDOQuery ordoQuery) throws Exception {
    List<String> result = new ArrayList<>();
    // "{diseases: { diseaseCode: { ontologyTermURI: {like:"
    return result;
  }
}

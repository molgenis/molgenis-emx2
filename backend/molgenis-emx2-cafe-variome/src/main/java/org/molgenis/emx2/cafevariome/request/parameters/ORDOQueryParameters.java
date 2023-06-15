package org.molgenis.emx2.cafevariome.request.parameters;

public class ORDOQueryParameters {
  public static final String ORDO_TERM_PAIRWISE_SIMILARITY =
      "jsonAPI[query][components][ordo][0][r]";
  public static final String ORDO_MATCH_SCALE = "jsonAPI[query][components][ordo][0][s]";
  public static final String ORDO_INCLUDE_HPO = "jsonAPI[query][components][ordo][0][HPO]";
  public static final String ORDO_SEARCH_TERM = "jsonAPI[query][components][ordo][0][id][]";
}

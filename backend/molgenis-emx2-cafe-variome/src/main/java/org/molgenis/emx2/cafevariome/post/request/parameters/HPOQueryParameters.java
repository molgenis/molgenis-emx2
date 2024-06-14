package org.molgenis.emx2.cafevariome.post.request.parameters;

public class HPOQueryParameters {
  public static final String HPO_TERM_PAIRWISE_SIMILARITY = "jsonAPI[query][components][sim][0][r]";
  public static final String HPO_MINIMUM_MATCHED_TERMS = "jsonAPI[query][components][sim][0][s]";
  public static final String HPO_INCLUDE_ORPHA = "jsonAPI[query][components][sim][0][ORPHA]";
  public static final String HPO_SEARCH_TERMS = "jsonAPI[query][components][sim][0][ids][]";
}

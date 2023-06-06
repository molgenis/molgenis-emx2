package org.molgenis.emx2.cafevariome.request.query;

import java.util.Arrays;

public class HPOQuery {

  private int termPairwiseSimilarity;
  private int minimumMatchedTerms;
  private boolean includeOrpha;
  private String[] searchTerms;

  public int getTermPairwiseSimilarity() {
    return termPairwiseSimilarity;
  }

  public void setTermPairwiseSimilarity(int termPairwiseSimilarity) {
    this.termPairwiseSimilarity = termPairwiseSimilarity;
  }

  public int getMinimumMatchedTerms() {
    return minimumMatchedTerms;
  }

  public void setMinimumMatchedTerms(int minimumMatchedTerms) {
    this.minimumMatchedTerms = minimumMatchedTerms;
  }

  public boolean isIncludeOrpha() {
    return includeOrpha;
  }

  public void setIncludeOrpha(boolean includeOrpha) {
    this.includeOrpha = includeOrpha;
  }

  public String[] getSearchTerms() {
    return searchTerms;
  }

  public void setSearchTerms(String[] searchTerms) {
    this.searchTerms = searchTerms;
  }

  @Override
  public String toString() {
    return "HPOQuery{"
        + "termPairwiseSimilarity="
        + termPairwiseSimilarity
        + ", minimumMatchedTerms="
        + minimumMatchedTerms
        + ", includeOrpha="
        + includeOrpha
        + ", searchTerms="
        + Arrays.toString(searchTerms)
        + '}';
  }
}

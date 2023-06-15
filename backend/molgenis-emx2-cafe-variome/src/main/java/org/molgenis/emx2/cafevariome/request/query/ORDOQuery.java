package org.molgenis.emx2.cafevariome.request.query;

public class ORDOQuery {

  private double termPairwiseSimilarity;
  private int matchScale;
  private boolean includeHPO;
  private String searchTerm;

  public double getTermPairwiseSimilarity() {
    return termPairwiseSimilarity;
  }

  public void setTermPairwiseSimilarity(double termPairwiseSimilarity) {
    this.termPairwiseSimilarity = termPairwiseSimilarity;
  }

  public int getMatchScale() {
    return matchScale;
  }

  public void setMatchScale(int matchScale) {
    this.matchScale = matchScale;
  }

  public boolean isIncludeHPO() {
    return includeHPO;
  }

  public void setIncludeHPO(boolean includeHPO) {
    this.includeHPO = includeHPO;
  }

  public String getSearchTerm() {
    return searchTerm;
  }

  public void setSearchTerm(String searchTerm) {
    this.searchTerm = searchTerm;
  }

  @Override
  public String toString() {
    return "ORDOQuery{"
        + "termPairwiseSimilarity="
        + termPairwiseSimilarity
        + ", matchScale="
        + matchScale
        + ", includeHPO="
        + includeHPO
        + ", searchTerm='"
        + searchTerm
        + '\''
        + '}';
  }
}

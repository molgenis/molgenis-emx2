package org.molgenis.emx2.cafevariome.post.request.query;

public class GeneReactomeQuery {

  private String id;
  private String[] proteinEffects;
  private int af;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String[] getProteinEffects() {
    return proteinEffects;
  }

  public void setProteinEffects(String[] proteinEffects) {
    this.proteinEffects = proteinEffects;
  }

  public int getAf() {
    return af;
  }

  public void setAf(int af) {
    this.af = af;
  }
}

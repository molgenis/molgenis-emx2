package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QueryComponentsReactome {

  private String reactom_id;
  private String[] protein_effect;
  private String af;

  public String getReactom_id() {
    return reactom_id;
  }

  public void setReactom_id(String reactom_id) {
    this.reactom_id = reactom_id;
  }

  public String[] getProtein_effect() {
    return protein_effect;
  }

  public void setProtein_effect(String[] protein_effect) {
    this.protein_effect = protein_effect;
  }

  public String getAf() {
    return af;
  }

  public void setAf(String af) {
    this.af = af;
  }

  @Override
  public String toString() {
    return "QueryComponentsReactome{"
        + "reactom_id='"
        + reactom_id
        + '\''
        + ", protein_effect="
        + Arrays.toString(protein_effect)
        + ", af='"
        + af
        + '\''
        + '}';
  }
}

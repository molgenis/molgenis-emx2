package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QueryComponentsOrdo {

  private String r;
  private String s;
  private String HPO;
  private String[] id;

  public String getR() {
    return r;
  }

  public void setR(String r) {
    this.r = r;
  }

  public String getS() {
    return s;
  }

  public void setS(String s) {
    this.s = s;
  }

  public String getHPO() {
    return HPO;
  }

  public void setHPO(String HPO) {
    this.HPO = HPO;
  }

  public String[] getId() {
    return id;
  }

  public void setId(String[] id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "QueryComponentsOrdo{"
        + "r='"
        + r
        + '\''
        + ", s='"
        + s
        + '\''
        + ", HPO='"
        + HPO
        + '\''
        + ", id="
        + Arrays.toString(id)
        + '}';
  }
}

package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QueryComponentsSimilarity {

  private String r;
  private String s;
  private String ORPHA;
  private String[] ids;

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

  public String getORPHA() {
    return ORPHA;
  }

  public void setORPHA(String ORPHA) {
    this.ORPHA = ORPHA;
  }

  public String[] getIds() {
    return ids;
  }

  public void setIds(String[] ids) {
    this.ids = ids;
  }

  @Override
  public String toString() {
    return "QueryComponentsSimilarity{"
        + "r='"
        + r
        + '\''
        + ", s='"
        + s
        + '\''
        + ", ORPHA='"
        + ORPHA
        + '\''
        + ", ids="
        + Arrays.toString(ids)
        + '}';
  }
}

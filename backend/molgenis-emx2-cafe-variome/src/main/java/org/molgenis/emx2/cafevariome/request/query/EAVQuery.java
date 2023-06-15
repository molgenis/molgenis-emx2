package org.molgenis.emx2.cafevariome.request.query;

import java.util.List;

public class EAVQuery {
  private List<EAVTriple> eavTriples;

  public List<EAVTriple> getEavTriples() {
    return eavTriples;
  }

  public void setEavTriples(List<EAVTriple> eavTriples) {
    this.eavTriples = eavTriples;
  }
}

package org.molgenis;

import org.molgenis.beans.QueryBean;

public interface Where {
  QueryBean eq(String... values);

  QueryBean eq(Integer... values);

  public enum Operator {
    EQ,
    OR
  }
}

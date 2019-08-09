package org.molgenis;

import java.io.Serializable;

public interface Where {
  Query eq(Serializable... values);

  Query contains(Serializable... values);

  Query search(String terms);

  Operator getOperator();

  Serializable[] getValues();

  String[] getPath();
}

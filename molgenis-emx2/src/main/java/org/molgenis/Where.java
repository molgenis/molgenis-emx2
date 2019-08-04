package org.molgenis;

import org.molgenis.beans.QueryBean;

import java.util.Map;
import java.util.UUID;

public interface Where {
  Query eq(Object... values);

  Query contains(Object... values);

  Query search(String terms);

  Operator getOperator();

  Object[] getValues();

  String[] getPath();
}

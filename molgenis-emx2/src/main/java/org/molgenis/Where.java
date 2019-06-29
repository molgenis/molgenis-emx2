package org.molgenis;

import org.molgenis.beans.QueryBean;

import java.util.Map;
import java.util.UUID;

public interface Where {
  Query eq(String... values);

  Query eq(Integer... values);

  Query eq(Double... values);

  Query eq(UUID... values);

  Operator getOperator();

  Object[] getValues();

  String[] getPath();
}

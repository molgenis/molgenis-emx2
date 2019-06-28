package org.molgenis;

import org.molgenis.beans.QueryBean;

import java.util.Map;

public interface Where {
  QueryBean eq(String... values);

  QueryBean eq(Integer... values);

  QueryBean eq(Double... values);

  Operator getOperator();

  Object[] getValues();

  String[] getPath();
}

package org.molgenis;

public interface Where {
  Query eq(Object... values);

  Query contains(Object... values);

  Query search(String terms);

  Operator getOperator();

  Object[] getValues();

  String[] getPath();
}

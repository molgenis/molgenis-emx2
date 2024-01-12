package org.molgenis.emx2;

import java.util.Collection;

public interface Filter {

  /**
   * if no column is provided, then we will assume this is a nested 'or', i.e. subfilter ||
   * subfilter ||...
   */
  String getColumn();

  Operator getOperator();

  /** will be 'or' for each value * */
  Object[] getValues();

  Filter getSubfilter(String column);

  Collection<Filter> getSubfilters();

  Filter addSubfilters(Filter... subfilters);
}

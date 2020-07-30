package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Filter {

  String getColumn();

  Operator getOperator();

  /** will be 'or' for each value * */
  Object[] getValues();

  Filter getSubfilter(String column);

  Collection<Filter> getSubfilters();

  Filter addSubfilters(Filter... subfilters);
}

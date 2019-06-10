package org.molgenis;

import java.util.Collection;

public interface Unique {

  Table getTable();

  Collection<Column> getColumns();

  Collection<String> getColumnNames();
}

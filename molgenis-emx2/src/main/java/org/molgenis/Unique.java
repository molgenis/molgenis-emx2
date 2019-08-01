package org.molgenis;

import java.util.Collection;

public interface Unique {

  Table getTable();

  Collection<String> getColumnNames();

  String getSchemaName();

  String getTableName();
}

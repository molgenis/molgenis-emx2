package org.molgenis.emx2.database;

import org.molgenis.sql.SqlDatabaseException;
import org.molgenis.sql.SqlRow;

import java.util.List;

public interface EmxQuery {
  List<SqlRow> fetch() throws SqlDatabaseException;
}

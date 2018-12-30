package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxColumn;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxType;
import org.molgenis.sql.SqlDatabase;
import org.molgenis.sql.SqlRow;
import org.molgenis.sql.SqlType;

import java.util.List;
import java.util.Map;

public class EmxTableImpl extends EmxTable {
  private SqlDatabase backend;
  private Map<String, EmxColumn> columns;

  public EmxTableImpl(EmxModel model, String name) {
    super(model, name);
  }

  @Override
  public EmxColumn addColumn(String name, EmxType type) {
    // if (getColumn()) backend.getTable(this.name).addColumn(name, convert(type));
    // reload();
    return getColumn(name);
  }

  private SqlType convert(EmxType type) {
    return SqlType.STRING;
  }

  public void reload(SqlRow sqlRow, List<SqlRow> sqlRows) {}
}

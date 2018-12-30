package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;
import org.molgenis.sql.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmxDatabase extends EmxModel {
  SqlDatabase backend;

  public EmxDatabase(SqlDatabase backend) throws EmxException {
    this.backend = backend;
    init();
  }

  @Override
  public EmxTable addTable(String name) throws EmxException {
    try {
      backend.createTable(name);
      save(getTable(name));
      reload();
      return getTable(name);
    } catch (Exception e) {
      throw new EmxException(e);
    }
  }

  private void save(EmxTable table) throws SqlDatabaseException {
    SqlRow tableMetadata = new SqlRow();
    tableMetadata.setString("name", table.getName());
    backend.getTable(table.getName()).update(tableMetadata);
  }

  private void init() throws EmxException {
    verifyTableMetadataStructure();
    verifyColumnMetadataStructure();
    reload();
  }

  private void verifyColumnMetadataStructure() {}

  private void verifyTableMetadataStructure() {}

  private void reload() throws EmxException {
    try {
      Map<String, SqlRow> tableMetadata = retrieveTableMetadata();
      Map<String, List<SqlRow>> columnMetadata = retrieveColumnMetadata();

      for (SqlTable st : backend.getTables()) {
        String name = st.getName();
        if (getTable(name) == null) {
          this.addTable(st.getName());
        } else {
          ((EmxTableImpl) getTable(name)).reload(tableMetadata.get(name), columnMetadata.get(name));
        }
      }
    } catch (Exception e) {
      throw new EmxException(e);
    }
  }

  private Map<String, List<SqlRow>> retrieveColumnMetadata() throws SqlQueryException {
    Map<String, List<SqlRow>> result = new LinkedHashMap<>();
    for (SqlRow columnMetadata : backend.getQuery().from("MOLGENIS_COLUMN_METADATA").retrieve()) {
      String table = columnMetadata.getString("table");
      if (result.get(table) == null) result.put(table, new ArrayList<SqlRow>());
      result.get(table).add(columnMetadata);
    }
    return result;
  }

  public Map<String, SqlRow> retrieveTableMetadata() throws SqlQueryException {
    Map<String, SqlRow> result = new LinkedHashMap<>();
    for (SqlRow tableMetadata : backend.getQuery().from("MOLGENIS_TABLE_METADATA").retrieve()) {
      String name = tableMetadata.getString("name");
      result.put(name, tableMetadata);
    }
    return result;
  }
}

package org.molgenis.emx2.database.internal;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;
import org.molgenis.sql.*;

import java.util.List;

class TableMetadataTable {
  final String TABLE_METADATA = "MOLGENIS_TABLE_METADATA";
  final String TABLE_NAME = "TABLE_NAME";
  final String TABLE_EXTENDS = "TABLE_EXTENDS";

  private SqlDatabase backend;

  public TableMetadataTable(SqlDatabase backend) throws SqlDatabaseException {
    this.backend = backend;
    this.verifyBackend();
  }

  private void verifyBackend() throws SqlDatabaseException {
    SqlTable columnTable = backend.getTable(TABLE_METADATA);
    if (columnTable.getColumn(TABLE_NAME) == null) {
      columnTable.addColumn(TABLE_NAME, SqlType.STRING);
    }
    columnTable.addUnique(TABLE_NAME);
  }

  public void reload(EmxModel model) throws SqlDatabaseException, EmxException {
    for (SqlRow tm : backend.getQuery().from(TABLE_METADATA).retrieve()) {
      EmxTable t = model.getTable(tm.getString(TABLE_NAME));
      if (tm.getString(TABLE_EXTENDS) != null) {
        t.setExtend(model.getTable(tm.getString(TABLE_EXTENDS)));
      }
      // TODO other metdata
    }
  }

  public void saveTable(EmxTable table) throws SqlDatabaseException {
    SqlRow tableMetadata = new SqlRow();
    tableMetadata.setString(TABLE_METADATA, table.getName());
    backend.getTable(table.getName()).update(tableMetadata);
  }

  public void deleteTable(String tableName) throws SqlDatabaseException {
    backend.getTable(TABLE_METADATA).update(find(tableName));
  }

  private SqlRow find(String tableName) throws SqlDatabaseException {
    List<SqlRow> rows =
        backend
            .getQuery()
            .from(TABLE_METADATA)
            .eq(TABLE_METADATA, TABLE_NAME, tableName)
            .retrieve();
    backend.getTable(TABLE_METADATA).delete(rows);
    if (rows.isEmpty()) return null;
    return rows.get(0);
  }
}

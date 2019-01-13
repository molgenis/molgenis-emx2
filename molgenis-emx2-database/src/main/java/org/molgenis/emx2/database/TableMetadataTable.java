package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;
import org.molgenis.sql.*;

import java.util.List;

class TableMetadataTable {
  private static final String TABLE_METADATA = "MOLGENIS_TABLE_METADATA";
  private static final String TABLE_NAME = "name";
  private static final String TABLE_EXTENDS = "extends";

  private SqlDatabaseImpl backend;

  public TableMetadataTable(SqlDatabaseImpl backend) throws SqlDatabaseException {
    this.backend = backend;
    this.verifyBackend();
  }

  private void verifyBackend() throws SqlDatabaseException {
    SqlTable columnTable = backend.getTable(TABLE_METADATA);
    if (columnTable == null) columnTable = backend.createTable(TABLE_METADATA);
    if (columnTable.getColumn(TABLE_NAME) == null) {
      columnTable.addColumn(TABLE_NAME, SqlType.STRING);
    }
    if (columnTable.getColumn(TABLE_EXTENDS) == null) {
      columnTable.addColumn(TABLE_EXTENDS, SqlType.STRING).setNullable(true);
    }
    if (!columnTable.isUnique(TABLE_NAME)) {
      columnTable.addUnique(TABLE_NAME);
    }
  }

  public void reload(EmxModel model) throws SqlDatabaseException, EmxException {
    for (SqlRow tm : backend.query(TABLE_METADATA).retrieve()) {
      EmxTable t = model.getTable(tm.getString(TABLE_NAME));
      if (t == null)
        throw new EmxException("metadata table out of sync for table " + tm.getString(TABLE_NAME));
      if (tm.getString(TABLE_EXTENDS) != null) {
        t.setExtend(model.getTable(tm.getString(TABLE_EXTENDS)));
      }
      // TODO other metdata
    }
  }

  public void saveTable(EmxTable table) throws SqlDatabaseException {
    List<SqlRow> rows =
        backend.query(TABLE_METADATA).eq(TABLE_METADATA, TABLE_NAME, table.getName()).retrieve();
    SqlRow tableMetadata = null;
    if (rows.isEmpty()) {
      tableMetadata = new SqlRow().setString(TABLE_NAME, table.getName());
    } else {
      tableMetadata = rows.get(0);
    }
    if (table.getExtend() != null) {
      tableMetadata.setString(TABLE_EXTENDS, table.getExtend().getName());
    }
    backend.getTable(TABLE_METADATA).update(tableMetadata);
  }

  public void deleteTable(String tableName) throws SqlDatabaseException {
    backend.getTable(TABLE_METADATA).delete(find(tableName));
  }

  private SqlRow find(String tableName) throws SqlDatabaseException {
    List<SqlRow> rows =
        backend.query(TABLE_METADATA).eq(TABLE_METADATA, TABLE_NAME, tableName).retrieve();
    if (rows.isEmpty()) return null;
    return rows.get(0);
  }
}

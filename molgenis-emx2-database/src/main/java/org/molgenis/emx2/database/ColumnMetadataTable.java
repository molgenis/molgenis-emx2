package org.molgenis.emx2.database;

import org.molgenis.emx2.*;
import org.molgenis.sql.*;

import java.util.List;

import static org.molgenis.emx2.EmxType.MREF;

class ColumnMetadataTable {
  private static final String COLUMN_METADATA_TABLE = "MOLGENIS_COLUMN_METADATA";
  private static final String COLUMN_TABLE = "table";
  private static final String COLUMN_NAME = "name";
  private static final String COLUMN_TYPE = "type";
  private static final String COLUMN_REF = "ref";
  private static final String COLUMN_JOINTABLE = "jointable";

  private SqlDatabaseImpl backend;

  public ColumnMetadataTable(SqlDatabaseImpl backend) throws SqlDatabaseException {
    this.backend = backend;
    this.verifyBackend();
  }

  private void verifyBackend() throws SqlDatabaseException {
    SqlTable columnTable = backend.getTable(COLUMN_METADATA_TABLE);
    if (columnTable == null) columnTable = backend.createTable(COLUMN_METADATA_TABLE);
    if (columnTable.getColumn(COLUMN_TABLE) == null) {
      columnTable.addColumn(COLUMN_TABLE, SqlType.STRING);
    }
    if (columnTable.getColumn(COLUMN_NAME) == null) {
      columnTable.addColumn(COLUMN_NAME, SqlType.STRING);
    }
    if (columnTable.getColumn(COLUMN_TYPE) == null) {
      columnTable.addColumn(COLUMN_TYPE, SqlType.STRING);
    }
    if (columnTable.getColumn(COLUMN_REF) == null) {
      columnTable.addColumn(COLUMN_REF, SqlType.STRING).setNullable(true);
    }
    if (columnTable.getColumn(COLUMN_JOINTABLE) == null) {
      columnTable.addColumn(COLUMN_JOINTABLE, SqlType.STRING).setNullable(true);
    }
    if (!columnTable.isUnique(COLUMN_TABLE, COLUMN_NAME)) {
      columnTable.addUnique(COLUMN_TABLE, COLUMN_NAME);
    }
  }

  public void reload(EmxModel model) throws SqlDatabaseException, EmxException {
    for (SqlRow row : backend.query(COLUMN_METADATA_TABLE).retrieve()) {
      EmxTable t = model.getTable(row.getString(COLUMN_TABLE));
      if (t == null)
        throw new EmxException(
            "column metadata table out of sync for table " + row.getString(COLUMN_TABLE));
      String name = row.getString(COLUMN_NAME);
      EmxType type = EmxType.valueOf(row.getString(COLUMN_TYPE));
      if (MREF.equals(type)) {
        EmxTable refTable = model.getTable(row.getString(COLUMN_REF));
        EmxTable joinTable = model.getTable(row.getString(COLUMN_JOINTABLE));
        t.addMref(row.getString(COLUMN_NAME), refTable, joinTable.getName());
      } else {
        EmxColumn c = t.addColumn(row.getString(COLUMN_NAME), type);
        if (t.getColumn(name) == null) {
          throw new EmxException(
              "column metadata table out of sync for column "
                  + row.getString(COLUMN_TABLE)
                  + "."
                  + row.getString(COLUMN_NAME));
          // TODO other attributes
        }
      }
    }
  }

  public void save(EmxColumn column) throws SqlDatabaseException {
    SqlRow tableMetadata = find(column);
    if (tableMetadata == null) tableMetadata = new SqlRow();
    tableMetadata.setString(COLUMN_TABLE, column.getTable().getName());
    tableMetadata.setString(COLUMN_NAME, column.getName());
    tableMetadata.setString(COLUMN_TYPE, column.getType().toString());
    if (column.getRef() != null) {
      tableMetadata.setString(COLUMN_REF, column.getTable().getName());
    }
    if (column.getJoinTable() != null) {
      tableMetadata.setString(COLUMN_JOINTABLE, column.getJoinTable().getName());
    }
    backend.getTable(COLUMN_METADATA_TABLE).update(tableMetadata);
  }

  public void removeColumn(EmxColumn column) throws SqlDatabaseException {
    backend.getTable(COLUMN_METADATA_TABLE).delete(find(column));
  }

  public void deleteColumnsForTable(String tableName) throws SqlDatabaseException {
    backend
        .getTable(COLUMN_METADATA_TABLE)
        .delete(
            backend
                .query(COLUMN_METADATA_TABLE)
                .eq(COLUMN_METADATA_TABLE, COLUMN_TABLE, tableName)
                .retrieve());
  }

  private SqlRow find(EmxColumn column) throws SqlDatabaseException {
    List<SqlRow> rows =
        backend
            .query(COLUMN_METADATA_TABLE)
            .eq(COLUMN_METADATA_TABLE, COLUMN_NAME, column.getName())
            .eq(COLUMN_METADATA_TABLE, COLUMN_TABLE, column.getTable().getName())
            .retrieve();
    if (rows.isEmpty()) return null;
    return rows.get(0);
  }
}

package org.molgenis.emx2.database.internal;

import org.molgenis.emx2.*;
import org.molgenis.sql.*;

public class EmxDatabaseModel extends EmxModel {
  SqlDatabase backend;
  TableMetadataTable tableMetadata;
  ColumnMetadataTable columnMetadata;
  boolean isReloading = false;

  public EmxDatabaseModel(SqlDatabase backend) throws EmxException {
    this.backend = backend;
    reload();
  }

  @Override
  public void removeTable(String tableName) throws EmxException {
    if (isReloading) return;
    try {
      backend.dropTable(tableName);
      columnMetadata.deleteColumnsForTable(tableName);
      tableMetadata.deleteTable(tableName);
      super.removeTable(tableName);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  @Override
  protected void onTableChange(EmxTable table) throws EmxException {
    if (isReloading) return;
    try {
      // ensure backend metadata in backend
      if (backend.getTable(table.getName()) == null) {
        backend.createTable(table.getName());
      }
      // store business logic metadata in metadata table
      tableMetadata.saveTable(table);
    } catch (Exception e) {
      throw new EmxException(e);
    }
  }

  @Override
  protected void onColumnChange(EmxColumn column) throws EmxException {
    if (isReloading) return;

    try {
      // ensure backend metadata in backend
      SqlTable table;
      if ((table = backend.getTable(column.getTable().getName())) == null) {
        throw new EmxException("Metadata model inconsistent with backend");
      }
      // TODO check if MREF column because those don't need to be in backend
      SqlColumn sqlColumn;
      if ((sqlColumn = table.getColumn(column.getName())) != null) {
        if (!sqlColumn.getType().equals(convert(column.getType()))) {
          throw new EmxException("Column definition inconsistent with backend");
        }
      } else {
        SqlType sqlType = convert(column.getType());
        if (SqlType.REF.equals(sqlType)) {
          sqlColumn =
              table.addColumn(column.getName(), backend.getTable(column.getTable().getName()));
        } else {
          sqlColumn = table.addColumn(column.getName(), sqlType);
        }
      }
      sqlColumn.setNullable(column.getNillable());
      // store business logic metadata in metadata table
      columnMetadata.save(column);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  @Override
  protected void onColumnRemove(EmxColumn column) throws EmxException {
    try {
      // TODO check if MREF column because those don't need to be in backend
      backend.getTable(column.getTable().getName()).removeColumn(column.getName());
      columnMetadata.removeColumn(column);
    } catch (Exception e) {
      throw new EmxException(e);
    }
  }

  private SqlType convert(EmxType type) {
    return SqlType.STRING;
  }

  private void reload() throws EmxException {
    try {
      isReloading = true;

      // reload core metadata from backend
      for (SqlTable st : backend.getTables()) {
        String name = st.getName();
        if (getTable(name) == null) {
          this.addTable(st.getName());
        }
      }
      // reload extended metadata from metadata tables
      tableMetadata.reload(this);
      columnMetadata.reload(this);

    } catch (Exception e) {
      throw new EmxException(e);
    } finally {
      isReloading = false;
    }
  }
}

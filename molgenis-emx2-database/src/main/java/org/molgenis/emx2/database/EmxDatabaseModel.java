package org.molgenis.emx2.database;

import org.molgenis.emx2.*;
import org.molgenis.sql.*;

import static org.molgenis.emx2.EmxType.MREF;
import static org.molgenis.emx2.EmxType.REF;
import static org.molgenis.sql.SqlRow.MOLGENISID;

public class EmxDatabaseModel extends EmxModel {
  private SqlDatabaseImpl backend;
  private TableMetadataTable tableMetadata;
  private ColumnMetadataTable columnMetadata;
  private boolean isReloading = false;

  public EmxDatabaseModel(SqlDatabaseImpl backend) throws SqlDatabaseException, EmxException {
    this.backend = backend;
    this.tableMetadata = new TableMetadataTable(backend);
    this.columnMetadata = new ColumnMetadataTable(backend);
    this.reload();
  }

  private void reload() throws SqlDatabaseException, EmxException {
    isReloading = true;
    // first load from backend
    for (SqlTable t : backend.getTables()) {
      EmxTable table = addTable(t.getName());
      for (SqlColumn c : t.getColumns()) {
        if (!MOLGENISID.equals(c.getName())) {
          EmxColumn col = table.addColumn(c.getName(), convert(c.getType()));
          col.setNillable(c.isNullable());
        }
      }
      for (SqlUnique u : t.getUniques()) {
        if (!u.getColumnNames().contains(MOLGENISID)) {
          table.addUnique(u.getColumnNames());
        }
      }
    }
    // then annotate from the metadata tables
    this.tableMetadata.reload(this);
    this.columnMetadata.reload(this);
    isReloading = false;
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

  @Override
  public void removeTable(String tableName) throws EmxException {
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
      if (!MREF.equals(column.getType())) {
        handleSimpleColumnChange(column, table);
      }
      columnMetadata.save(column);

    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  private void handleSimpleColumnChange(EmxColumn column, SqlTable table)
      throws EmxException, SqlDatabaseException {
    SqlColumn sqlColumn;
    if ((sqlColumn = table.getColumn(column.getName())) != null) {
      if (!sqlColumn.getType().equals(convert(column.getType()))) {
        throw new EmxException("Column metadata inconsistent with backend");
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
  }

  private SqlType convert(EmxType type) {
    switch (type) {
      case STRING:
        return SqlType.STRING;
      case INT:
        return SqlType.INT;
      case BOOL:
        return SqlType.BOOL;
      case DECIMAL:
        return SqlType.DECIMAL;
      case TEXT:
        return SqlType.TEXT;
      case DATE:
        return SqlType.DATE;
      case DATETIME:
        return SqlType.DATETIME;
      case UUID:
      case REF:
        return SqlType.UUID;
      default:
        throw new UnsupportedOperationException();
    }
  }

  private EmxType convert(SqlType type) {
    switch (type) {
      case STRING:
        return EmxType.STRING;
      case INT:
        return EmxType.INT;
      case BOOL:
        return EmxType.BOOL;
      case DECIMAL:
        return EmxType.DECIMAL;
      case TEXT:
        return EmxType.TEXT;
      case DATE:
        return EmxType.DATE;
      case DATETIME:
        return EmxType.DATETIME;
      case UUID:
        return EmxType.UUID;
      default:
        throw new UnsupportedOperationException();
    }
  }
}

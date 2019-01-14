package org.molgenis.emx2.database;

import org.molgenis.emx2.*;
import org.molgenis.sql.*;

import javax.sql.DataSource;
import java.util.*;

import static org.molgenis.emx2.EmxType.MREF;

public class EmxDatabaseImpl implements EmxDatabase {
  private SqlDatabaseImpl backend;
  private EmxDatabaseModel model;

  public EmxDatabaseImpl(DataSource ds) throws EmxException {
    try {
      this.backend = new SqlDatabaseImpl(ds);
      this.model = new EmxDatabaseModel(backend);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  @Override
  public EmxModel getModel() {
    return model;
  }

  @Override
  public EmxQuery query(String tableName) throws EmxException {
    return new QueryImpl(this.backend, tableName);
  }

  @Override
  public EmxRow findById(String tableName, UUID id) {
    return null;
  }

  @Override
  public void save(String tableName, SqlRow row) throws EmxException {
    this.save(tableName, Arrays.asList(row));
  }

  @Override
  public int save(String tableName, Collection<SqlRow> rows) throws EmxException {
    try {
      int count = backend.getTable(tableName).update(rows);
      for (EmxColumn column : model.getTable(tableName).getColumns()) {
        if (MREF.equals(column.getType())) {
          deleteOldMrefs(tableName, rows, column);
          saveUpdatedMrefs(tableName, rows, column);
        }
      }
      return count;
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  private void deleteOldMrefs(String tableName, Collection<SqlRow> rows, EmxColumn column)
      throws SqlDatabaseException {
    String joinTable = column.getJoinTable().getName();
    List<UUID> oldMrefIds = new ArrayList<>();
    for (SqlRow r : rows) {
      oldMrefIds.add(r.getRowID());
    }
    List<SqlRow> oldMrefs =
        backend
            .query(joinTable)
            .eq(joinTable, tableName, oldMrefIds.toArray(new UUID[oldMrefIds.size()]))
            .retrieve();
    backend.getTable(joinTable).delete(oldMrefs);
  }

  private void saveUpdatedMrefs(String tableName, Collection<SqlRow> rows, EmxColumn column)
      throws SqlDatabaseException {
    String colName = column.getName();
    String otherTable = column.getRef().getName();
    String joinTable = column.getJoinTable().getName();

    List<SqlRow> newMrefs = new ArrayList<>();
    for (SqlRow r : rows) {
      for (SqlRow ref : r.getMref(colName)) {
        SqlRow join = new SqlRow().setRef(tableName, r).setRef(otherTable, ref);
        newMrefs.add(join);
      }
    }
    backend.getTable(joinTable).update(newMrefs);
  }

  @Override
  public int delete(String tableName, Collection<SqlRow> rows) throws EmxException {
    try {
      for (EmxColumn column : model.getTable(tableName).getColumns()) {
        if (MREF.equals(column.getType())) {
          deleteOldMrefs(tableName, rows, column);
        }
      }
      return backend.getTable(tableName).delete(rows);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  @Override
  public void delete(String tableName, SqlRow row) throws EmxException {
    this.delete(tableName, Arrays.asList(row));
  }
}

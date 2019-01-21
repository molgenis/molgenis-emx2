package org.molgenis.emx2.database;

import org.molgenis.emx2.*;
import org.molgenis.sql.*;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

import static org.molgenis.emx2.EmxType.MREF;

public class EmxDatabaseImpl implements EmxDatabase {
  private SqlDatabaseImpl backend;
  private EmxDatabaseModel model;

  public EmxDatabaseImpl(DataSource ds) throws SqlDatabaseException, EmxException {
    try {
      this.backend = new SqlDatabaseImpl(ds);
      this.model = new EmxDatabaseModel(backend);
    } catch (SqlDatabaseException e) {
      throw new SqlDatabaseException(e);
    }
  }

  @Override
  public EmxModel getModel() {
    return model;
  }

  @Override
  public SqlQuery query(String tableName) throws SqlDatabaseException {
    return this.backend.query(tableName);
  }

  @Override
  public SqlRow findById(String tableName, UUID id) {
    return null;
  }

  @Override
  public void save(String tableName, SqlRow row) throws SqlDatabaseException {
    this.save(tableName, Arrays.asList(row));
  }

  @Override
  public int save(String tableName, Collection<SqlRow> rows) throws SqlDatabaseException {
    try {
      int count = backend.getTable(tableName).update(convert(rows));
      for (EmxColumn column : model.getTable(tableName).getColumns()) {
        if (MREF.equals(column.getType())) {
          deleteOldMrefs(tableName, rows, column);
          saveUpdatedMrefs(tableName, rows, column);
        }
      }
      return count;
    } catch (SqlDatabaseException e) {
      throw new SqlDatabaseException(e);
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
        this.query(joinTable)
            .eq(joinTable, tableName, oldMrefIds.toArray(new UUID[oldMrefIds.size()]))
            .retrieve();
    this.delete(joinTable, oldMrefs);
  }

  private void saveUpdatedMrefs(String tableName, Collection<SqlRow> rows, EmxColumn column)
      throws SqlDatabaseException {
    String colName = column.getName();
    String otherTable = column.getRef().getName();
    String joinTable = column.getJoinTable().getName();

    List<SqlRow> newMrefs = new ArrayList<>();
    for (SqlRow r : rows) {
      for (UUID ref : r.getMref(colName)) {
        SqlRow join = new SqlRow().setRef(tableName, convert(r)).setRef(otherTable, ref);
        newMrefs.add(join);
      }
    }
    backend.getTable(joinTable).update(newMrefs);
  }

  @Override
  public int delete(String tableName, Collection<SqlRow> rows) throws SqlDatabaseException {
    try {
      List<SqlRow> sqlRows = convert(rows);
      for (EmxColumn column : model.getTable(tableName).getColumns()) {
        if (MREF.equals(column.getType())) {
          deleteOldMrefs(tableName, rows, column);
        }
      }
      return backend.getTable(tableName).delete(sqlRows);
    } catch (SqlDatabaseException e) {
      throw new SqlDatabaseException(e);
    }
  }

  @Override
  public void delete(String tableName, SqlRow row) throws SqlDatabaseException {
    this.delete(tableName, Arrays.asList(row));
  }

  private List<SqlRow> convert(Collection<SqlRow> rows) {
    return rows.stream().map(row -> convert(row)).collect(Collectors.toList());
  }

  private SqlRow convert(SqlRow row) {
    Map<String, Object> valueMap = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : row.getValueMap().entrySet()) {
      if (entry.getValue() instanceof SqlRow) {
        valueMap.put(entry.getKey(), convert((SqlRow) entry.getValue()));
      } else {
        valueMap.put(entry.getKey(), entry.getValue());
      }
    }
    return new SqlRow(valueMap);
  }
}

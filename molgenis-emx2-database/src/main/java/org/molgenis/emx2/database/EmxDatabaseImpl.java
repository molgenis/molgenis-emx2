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
  public void save(String tableName, EmxRow row) throws EmxException {
    this.save(tableName, Arrays.asList(row));
  }

  @Override
  public int save(String tableName, Collection<EmxRow> rows) throws EmxException {
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
      throw new EmxException(e);
    }
  }

  private void deleteOldMrefs(String tableName, Collection<EmxRow> rows, EmxColumn column)
      throws EmxException {
    String joinTable = column.getJoinTable().getName();
    List<UUID> oldMrefIds = new ArrayList<>();
    for (EmxRow r : rows) {
      oldMrefIds.add(r.getRowID());
    }
    List<EmxRow> oldMrefs =
        this.query(joinTable)
            .eq(joinTable, tableName, oldMrefIds.toArray(new UUID[oldMrefIds.size()]))
            .fetch();
    this.delete(joinTable, oldMrefs);
  }

  private void saveUpdatedMrefs(String tableName, Collection<EmxRow> rows, EmxColumn column)
      throws SqlDatabaseException {
    String colName = column.getName();
    String otherTable = column.getRef().getName();
    String joinTable = column.getJoinTable().getName();

    List<SqlRow> newMrefs = new ArrayList<>();
    for (EmxRow r : rows) {
      for (EmxRow ref : r.getMref(colName)) {
        SqlRow join = new SqlRow().setRef(tableName, convert(r)).setRef(otherTable, convert(ref));
        newMrefs.add(join);
      }
    }
    backend.getTable(joinTable).update(newMrefs);
  }

  @Override
  public int delete(String tableName, Collection<EmxRow> rows) throws EmxException {
    try {
      List<SqlRow> sqlRows = convert(rows);
      for (EmxColumn column : model.getTable(tableName).getColumns()) {
        if (MREF.equals(column.getType())) {
          deleteOldMrefs(tableName, rows, column);
        }
      }
      return backend.getTable(tableName).delete(sqlRows);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  @Override
  public void delete(String tableName, EmxRow row) throws EmxException {
    this.delete(tableName, Arrays.asList(row));
  }

  private List<SqlRow> convert(Collection<EmxRow> rows) {
    return rows.stream().map(row -> convert(row)).collect(Collectors.toList());
  }

  private SqlRow convert(EmxRow row) {
    Map<String, Object> valueMap = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : row.getValueMap().entrySet()) {
      if (entry.getValue() instanceof EmxRow) {
        valueMap.put(entry.getKey(), convert((EmxRow) entry.getValue()));
      } else {
        valueMap.put(entry.getKey(), entry.getValue());
      }
    }
    return new SqlRow(valueMap);
  }
}

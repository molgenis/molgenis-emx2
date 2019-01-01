package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxRow;
import org.molgenis.emx2.database.internal.EmxDatabaseModel;
import org.molgenis.sql.SqlDatabase;
import org.molgenis.sql.SqlDatabaseException;
import org.molgenis.sql.SqlRow;

import java.util.*;
import java.util.stream.Stream;

public class EmxDatabase {
  SqlDatabase backend;
  EmxModel model;

  public EmxDatabase(SqlDatabase backend) throws EmxException {
    this.backend = backend;
    this.model = new EmxDatabaseModel(backend);
  }

  public EmxModel model() {
    return model;
  }

  public Stream<EmxRow> find(String tableName, EmxFilter... filters) {
    return null;
  }

  public EmxRow findById(String tableName, UUID id) {
    return null;
  }

  public void save(String tableName, EmxRow row) throws EmxException {
    this.save(tableName, Arrays.asList(row));
  }

  public int save(String tableName, Collection<EmxRow> rows) throws EmxException {
    try {
      // TODO, deal with mref/one-to-many relationships
      List<SqlRow> sqlRows = new ArrayList<>();
      rows.forEach(row -> sqlRows.add(new SqlRow(row.toMap())));
      return backend.getTable(tableName).update(sqlRows);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  public int delete(String tableName, Collection<EmxRow> rows) throws EmxException {
    int count = 0;
    try {
      // TODO, deal with mref/one-to-many relationships
      List<SqlRow> ids = new ArrayList<>(rows.size());
      rows.forEach(row -> ids.add(new SqlRow(row.getId())));
      return backend.getTable(tableName).delete(ids);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  public void delete(String tableName, EmxRow row) throws EmxException {
    this.delete(tableName, Arrays.asList(row));
  }
}

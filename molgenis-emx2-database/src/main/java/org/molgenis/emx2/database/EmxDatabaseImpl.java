package org.molgenis.emx2.database;

import org.molgenis.emx2.*;
import org.molgenis.sql.*;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class EmxDatabaseImpl implements EmxDatabase {
  private SqlDatabaseImpl backend;
  private EmxDatabaseModel model;

  public EmxDatabaseImpl(DataSource ds) throws EmxException {
    this.backend = new SqlDatabaseImpl(ds);
    try {
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
  public EmxQuery query(String tableName) {
    return null;
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
      // TODO, deal with mref/one-to-many relationships
      return backend.getTable(tableName).update(rows);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  @Override
  public int delete(String tableName, Collection<SqlRow> rows) throws EmxException {
    try {
      // TODO, deal with mref/one-to-many relationships
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

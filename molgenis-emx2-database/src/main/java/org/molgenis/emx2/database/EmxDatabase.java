package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxRow;

import java.util.Collection;
import java.util.UUID;

public interface EmxDatabase {
  EmxModel getModel();

  EmxQuery query(String tableName) throws EmxException;

  EmxRow findById(String tableName, UUID id);

  void save(String tableName, EmxRow row) throws EmxException;

  int save(String tableName, Collection<EmxRow> rows) throws EmxException;

  int delete(String tableName, Collection<EmxRow> rows) throws EmxException;

  void delete(String tableName, EmxRow row) throws EmxException;
}

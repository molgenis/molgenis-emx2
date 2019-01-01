package org.molgenis.emx2;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

public interface EmxDatabase {
  Stream<EmxRow> find(String tableName, EmxFilter... filters);

  EmxRow findById(String tableName, UUID id);

  void save(String tableName, EmxRow row) throws EmxException;

  int save(String tableName, Collection<EmxRow> rows) throws EmxException;

  int delete(String tableName, Collection<EmxRow> rows) throws EmxException;

  void delete(String tableName, EmxRow row) throws EmxException;
}

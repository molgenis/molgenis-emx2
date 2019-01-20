package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxRow;

import java.util.List;
import java.util.UUID;

public interface EmxQuery {
  List<EmxRow> fetch() throws EmxException;

  EmxQuery eq(String joinTable, String tableName, UUID... uuids) throws EmxException;
}

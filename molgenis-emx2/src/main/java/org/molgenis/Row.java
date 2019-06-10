package org.molgenis;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Row {
  static final String MOLGENISID = "molgenisid";

  UUID getRowID();

  Row setRowID(UUID id);

  UUID getUuid(String columnId);

  String getString(String name);

  Integer getInt(String name);

  Boolean getBool(String name);

  Double getDecimal(String name);

  String getText(String name);

  LocalDate getDate(String name);

  OffsetDateTime getDateTime(String name);

  UUID getRef(String name);

  void set(Map<String, Object> values);

  Row setString(String name, String value);

  Row setInt(String name, Integer value);

  Row setRef(String name, Row value);

  Row setMref(String name, List<Row> values);

  Row setMref(String colName, Row... values);

  Row setRef(String name, UUID value);

  Row setDecimal(String columnId, Double value);

  Row setBool(String columnId, Boolean value);

  Row setDate(String columnId, LocalDate value);

  Row setDateTime(String columnId, OffsetDateTime value);

  Row setText(String columnId, String value);

  Row setUuid(String columnId, UUID value);

  List values(String... columns);

  String toString();

  Map<String, Object> getValueMap();

  List<UUID> getMref(String colName);
}

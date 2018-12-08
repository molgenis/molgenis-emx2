package org.molgenis.sql;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SqlRow {
    List values(String... columns);

    UUID getRowID();

    SqlRow setRowID(UUID id);

    UUID getUuid(String columnId);

    String getString(String columnId);

    Integer getInt(String columnId);

    Boolean getBool(String columnId);

    Double getDecimal(String columnId);

    String getText(String columnId);

    LocalDate getDate(String columnId);

    OffsetDateTime getDateTime(String columnId);

    UUID getRef(String columnId);

    SqlRow setString(String columnId, String value);

    SqlRow setInt(String columnId, Integer value);

    SqlRow setRef(String columnId, SqlRow ref);

    SqlRow setRef(String columnId, UUID ref);

    SqlRow setDecimal(String columnId, Double value);

    SqlRow setBool(String columnId, Boolean value);

    SqlRow setDate(String columnId, LocalDate value);

    SqlRow setDateTime(String columnId, OffsetDateTime value);

    SqlRow setText(String columnId, String value);

    SqlRow setUuid(String columnId, UUID value);
}

package org.molgenis.sql.psql;

import org.jooq.Field;
import org.jooq.Record;
import org.molgenis.sql.SqlRow;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.molgenis.sql.psql.PsqlDatabase.MOLGENISID;

public class PsqlRow implements SqlRow {
  private Map<String, Object> values = new LinkedHashMap<>();

  public PsqlRow() {
    this(UUID.randomUUID());
  }

  public PsqlRow(Record record) {
    for (Field f : record.fields()) {
      values.put(f.getName(), record.get(f));
    }
  }

  public PsqlRow(UUID molgenisid) {
    if (molgenisid != null) {
      this.values.put(MOLGENISID, molgenisid);
    }
  }

  @Override
  public UUID getRowID() {
    return (UUID) values.get(MOLGENISID);
  }

  @Override
  public SqlRow setRowID(UUID id) {
    this.setRef(MOLGENISID, id);
    return this;
  }

  @Override
  public UUID getUuid(String columnId) {
    return (UUID) this.values.get(columnId);
  }

  @Override
  public String getString(String name) {
    return (String) values.get(name);
  }

  @Override
  public Integer getInt(String name) {
    return (Integer) values.get(name);
  }

  @Override
  public Boolean getBool(String name) {
    return (Boolean) values.get(name);
  }

  @Override
  public Double getDecimal(String name) {
    return (Double) values.get(name);
  }

  @Override
  public String getText(String name) {
    return (String) values.get(name);
  }

  @Override
  public LocalDate getDate(String name) {
    if (values.get(name) == null) return null;
    if (values.get(name) instanceof Date) {
      return LocalDate.parse(values.get(name).toString());
    } else if (values.get(name) instanceof OffsetDateTime) {
      return ((OffsetDateTime) values.get(name)).toLocalDate();
    } else {
      return (LocalDate) values.get(name);
    }
  }

  @Override
  public OffsetDateTime getDateTime(String name) {
    return (OffsetDateTime) values.get(name);
  }

  @Override
  public UUID getRef(String name) {
    return (UUID) values.get(name);
  }

  @Override
  public SqlRow setString(String name, String value) {
    values.put(name, value);
    return this;
  }

  @Override
  public SqlRow setInt(String name, Integer value) {
    values.put(name, value);
    return this;
  }

  @Override
  public SqlRow setRef(String name, SqlRow value) {
    values.put(name, value.getRowID());
    return this;
  }

  @Override
  public SqlRow setRef(String name, UUID value) {
    values.put(name, value);
    return this;
  }

  @Override
  public SqlRow setDecimal(String columnId, Double value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public SqlRow setBool(String columnId, Boolean value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public SqlRow setDate(String columnId, LocalDate value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public SqlRow setDateTime(String columnId, OffsetDateTime value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public SqlRow setText(String columnId, String value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public SqlRow setUuid(String columnId, UUID value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public List values(String... columns) {
    List<Object> result = new ArrayList<>();
    for (String name : columns) {
      result.add(values.get(name));
    }
    return Collections.unmodifiableList(result);
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ROW(");
    for (Map.Entry<String, Object> col : values.entrySet()) {
      builder.append(col.getKey()).append("='").append(col.getValue()).append("' ");
    }
    builder.append(")");
    return builder.toString();
  }
}

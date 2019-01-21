package org.molgenis.sql;

import com.fasterxml.uuid.Generators;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

public class SqlRow {
  public static final String MOLGENISID = "molgenisid";
  protected Map<String, Object> values = new LinkedHashMap<>();
  protected Map<String, List<UUID>> mrefs = new LinkedHashMap<>();

  public SqlRow(Map<String, Object> values) {
    this();
    this.values.putAll(values);
  }

  public SqlRow() { // to ensure we have nicely sorted record we have time based uuid
    this(Generators.timeBasedGenerator().generate());
  }

  public SqlRow(UUID id) {
    setRowID(id);
  }

  public UUID getRowID() {
    return (UUID) values.get(MOLGENISID);
  }

  public SqlRow setRowID(UUID id) {
    this.setRef(MOLGENISID, id);
    return this;
  }

  public UUID getUuid(String columnId) {
    return (UUID) this.values.get(columnId);
  }

  public String getString(String name) {
    return (String) values.get(name);
  }

  public Integer getInt(String name) {
    return (Integer) values.get(name);
  }

  public Boolean getBool(String name) {
    return (Boolean) values.get(name);
  }

  public Double getDecimal(String name) {
    return (Double) values.get(name);
  }

  public String getText(String name) {
    return (String) values.get(name);
  }

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

  public OffsetDateTime getDateTime(String name) {
    return (OffsetDateTime) values.get(name);
  }

  public UUID getRef(String name) {
    return (UUID) values.get(name);
  }

  public SqlRow setString(String name, String value) {
    values.put(name, value);
    return this;
  }

  public SqlRow setInt(String name, Integer value) {
    values.put(name, value);
    return this;
  }

  public SqlRow setRef(String name, SqlRow value) {
    values.put(name, value.getRowID());
    return this;
  }

  public SqlRow setMref(String name, UUID... values) {
    mrefs.put(name, Arrays.asList(values));
    return this;
  }

  public SqlRow setRef(String name, UUID value) {
    values.put(name, value);
    return this;
  }

  public SqlRow setDecimal(String columnId, Double value) {
    values.put(columnId, value);
    return this;
  }

  public SqlRow setBool(String columnId, Boolean value) {
    values.put(columnId, value);
    return this;
  }

  public SqlRow setDate(String columnId, LocalDate value) {
    values.put(columnId, value);
    return this;
  }

  public SqlRow setDateTime(String columnId, OffsetDateTime value) {
    values.put(columnId, value);
    return this;
  }

  public SqlRow setText(String columnId, String value) {
    values.put(columnId, value);
    return this;
  }

  public SqlRow setUuid(String columnId, UUID value) {
    values.put(columnId, value);
    return this;
  }

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
    for (Map.Entry<String, List<UUID>> col : mrefs.entrySet()) {
      builder.append(col.getKey()).append("='").append(col.getValue()).append("' ");
    }
    builder.append(")");
    return builder.toString();
  }

  public Map<String, Object> getValueMap() {
    return values;
  }

  public SqlRow setMref(String colName, SqlRow... values) {
    List<UUID> ids = new ArrayList<>();
    for (SqlRow r : values) {
      ids.add(r.getRowID());
    }
    this.mrefs.put(colName, ids);
    return this;
  }

  public List<UUID> getMref(String colName) {
    return this.mrefs.get(colName);
  }
}

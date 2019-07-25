package org.molgenis;

import com.fasterxml.uuid.Generators;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.molgenis.Database.RowLevelSecurity.MG_EDIT_ROLE;

public class Row implements Identifiable {

  public static final String MOLGENISID = "molgenisid";
  private Map<String, Object> values = new LinkedHashMap<>();
  private Map<String, List<UUID>> mrefs = new LinkedHashMap<>();

  public Row() { // to ensure we have nicely sorted record we have time based uuid
    this(Generators.timeBasedGenerator().generate());
  }

  public Row(Map<String, ?> values) {
    this.values.putAll(values);
  }

  public Row(UUID id) {
    setMolgenisid(id);
  }

  public UUID getMolgenisid() {
    return (UUID) values.get(MOLGENISID);
  }

  public org.molgenis.Row setMolgenisid(UUID id) {
    this.setRef(MOLGENISID, id);
    return this;
  }

  public UUID getUuid(String name) {
    Object v = values.get(name);
    try {
      if (v instanceof String) return UUID.fromString((String) v);
      return (UUID) v;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Row.getUuid(\"" + name + "\") failed: value is '" + v + "'");
    }
  }

  public String getEnum(String name) {
    return getString(name);
  }

  public String getString(String name) {
    Object v = values.get(name);
    if (v == null) return null;
    if (v instanceof String) return (String) v;
    return v.toString();
  }

  public Integer getInt(String name) {
    Object v = values.get(name);
    try {
      if (v instanceof String) return Integer.parseInt((String) v);
      return (Integer) v;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Row.getInt(\"" + name + "\") failed: value is '" + v + "'");
    }
  }

  public Boolean getBool(String name) {
    Object v = values.get(name);
    try {
      if (v instanceof String) {
        if ("true".equalsIgnoreCase((String) v)) return true;
        if ("false".equalsIgnoreCase((String) v)) return false;
      }
      return (Boolean) v;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Row.getBool(\"" + name + "\") failed: value is '" + v + "'");
    }
  }

  public Double getDecimal(String name) {
    Object v = values.get(name);
    try {
      if (v instanceof String) return Double.parseDouble((String) v);
      return (Double) v;
    } catch (Exception e) {
      throw new NumberFormatException(
          "Row.getDecimal(\"" + name + "\") failed: value is '" + v + "'");
    }
  }

  public String getText(String name) {
    return getString(name);
  }

  public LocalDate getDate(String name) {
    Object v = values.get(name);
    try {
      if (v == null) return null;
      if (v instanceof LocalDate) return (LocalDate) v;
      if (v instanceof OffsetDateTime) {
        return ((OffsetDateTime) v).toLocalDate();
      } else {

        return LocalDate.parse(v.toString());
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Row.getDate(\"" + name + "\") failed: " + e.getMessage());
    }
  }

  public LocalDateTime getDateTime(String name) {
    Object v = values.get(name);
    try {
      if (v == null) return null;
      if (v instanceof LocalDateTime) return (LocalDateTime) v;
      if (v instanceof OffsetDateTime) return ((OffsetDateTime) v).toLocalDateTime();
      if (v instanceof Timestamp) return ((Timestamp) v).toLocalDateTime();
      return LocalDateTime.parse(v.toString());
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Row.getDateTime(\""
              + name
              + "\") failed. Type was "
              + v.getClass()
              + ". Error:"
              + e.getMessage());
    }
  }

  public UUID getRef(String name) {
    return getUuid(name);
  }

  public void set(Map<String, Object> values) {
    this.values.putAll(values);
  }

  public Row set(String name, Object value) {
    this.values.put(name, value);
    return this;
  }

  public org.molgenis.Row setString(String name, String value) {
    values.put(name, value);
    return this;
  }

  public org.molgenis.Row setEnum(String name, String value) {
    values.put(name, value);
    return this;
  }

  public org.molgenis.Row setInt(String name, Integer value) {
    values.put(name, value);
    return this;
  }

  public org.molgenis.Row setRef(String name, org.molgenis.Row value) {
    values.put(name, value.getMolgenisid());
    return this;
  }

  public org.molgenis.Row setMref(String name, List<org.molgenis.Row> values) {
    List<UUID> uuids = new ArrayList<>();
    for (org.molgenis.Row r : values) uuids.add(r.getMolgenisid());
    mrefs.put(name, uuids);
    return this;
  }

  public org.molgenis.Row setMref(String colName, org.molgenis.Row... values) {
    List<UUID> ids = new ArrayList<>();
    for (org.molgenis.Row r : values) {
      ids.add(r.getMolgenisid());
    }
    mrefs.put(colName, ids);
    return this;
  }

  public org.molgenis.Row setRef(String name, UUID value) {
    values.put(name, value);
    return this;
  }

  public org.molgenis.Row setDecimal(String columnId, Double value) {
    values.put(columnId, value);
    return this;
  }

  public org.molgenis.Row setBool(String columnId, Boolean value) {
    values.put(columnId, value);
    return this;
  }

  public org.molgenis.Row setDate(String columnId, LocalDate value) {
    values.put(columnId, value);
    return this;
  }

  public org.molgenis.Row setDateTime(String columnId, LocalDateTime value) {
    values.put(columnId, value);
    return this;
  }

  public org.molgenis.Row setText(String columnId, String value) {
    values.put(columnId, value);
    return this;
  }

  public org.molgenis.Row setUuid(String columnId, UUID value) {
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

  public List<UUID> getMref(String colName) {
    return this.mrefs.get(colName);
  }

  public Collection<String> getColumns() {
    return values.keySet();
  }

  public org.molgenis.Row setRowEditRole(String role) {
    return this.setString(MG_EDIT_ROLE.toString(), role);
  }
}

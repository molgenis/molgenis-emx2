package org.molgenis;

import com.fasterxml.uuid.Generators;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

  public Row setMolgenisid(UUID id) {
    this.setRef(MOLGENISID, id);
    return this;
  }

  public UUID getUuid(String name) {
    return TypeUtils.toUuid(values.get(name));
  }

  public UUID[] getUuidArray(String name) {
    return TypeUtils.toUuidArray(values.get(name));
  }

  public String getString(String name) {
    return TypeUtils.toString(values.get(name));
  }

  public String[] getStringArray(String name) {
    return TypeUtils.toStringArray(values.get(name));
  }

  public Integer getInt(String name) {
    return TypeUtils.toInt(values.get(name));
  }

  public Integer[] getIntArray(String name) {
    return TypeUtils.toIntArray(values.get(name));
  }

  public Boolean getBool(String name) {
    return TypeUtils.toBool(values.get(name));
  }

  public Boolean[] getBoolArray(String name) {
    return TypeUtils.toBoolArray(values.get(name));
  }

  public Double getDecimal(String name) {
    return TypeUtils.toDecimal(values.get(name));
  }

  public Double[] getDecimalArray(String name) {
    return TypeUtils.toDecimalArray(values.get(name));
  }

  public String getText(String name) {
    return getString(name);
  }

  public String[] getTextArray(String name) {
    return getStringArray(name);
  }

  public LocalDate getDate(String name) {
    return TypeUtils.toDate(values.get(name));
  }

  public LocalDate[] getDateArray(String name) {
    return TypeUtils.toDateArrray(values.get(name));
  }

  public LocalDateTime getDateTime(String name) {
    return TypeUtils.toDateTime(values.get(name));
  }

  public LocalDateTime[] getDateTimeArray(String name) {
    return TypeUtils.toDateTimeArray(values.get(name));
  }

  public void set(Map<String, Object> values) {
    this.values.putAll(values);
  }

  public Row set(String name, Object value) {
    this.values.put(name, value);
    return this;
  }

  public Row setString(String name, String value) {
    this.values.put(name, value);
    return this;
  }

  public Row setStringArray(String name, String[] value) {
    this.values.put(name, value);
    return this;
  }

  public Row setInt(String name, Integer value) {
    this.values.put(name, value);
    return this;
  }

  public Row setRef(String name, Row value) {
    this.values.put(name, value.getMolgenisid());
    return this;
  }

  public Row setMref(String name, List<Row> values) {
    List<UUID> uuids = new ArrayList<>();
    for (Row r : values) uuids.add(r.getMolgenisid());
    mrefs.put(name, uuids);
    return this;
  }

  public Row setMref(String colName, Row... values) {
    List<UUID> ids = new ArrayList<>();
    for (Row r : values) {
      ids.add(r.getMolgenisid());
    }
    mrefs.put(colName, ids);
    return this;
  }

  public Row setRef(String name, UUID value) {
    this.values.put(name, value);
    return this;
  }

  public Row setDecimal(String columnId, Double value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setBool(String columnId, Boolean value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setDate(String columnId, LocalDate value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setDateTime(String columnId, LocalDateTime value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setText(String columnId, String value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setUuid(String columnId, UUID value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setRefArray(String column, Object... values) {
    this.values.put(column, values);
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
    return this.values;
  }

  public List<UUID> getMref(String colName) {
    return this.mrefs.get(colName);
  }

  public Collection<String> getColumns() {
    return this.values.keySet();
  }

  public Row setRowEditRole(String role) {
    return this.setString(MG_EDIT_ROLE.toString(), role);
  }
}

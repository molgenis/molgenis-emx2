package org.molgenis;

import com.fasterxml.uuid.Generators;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Row implements Identifiable {

  public static final String MOLGENISID = "molgenisid";
  private Map<String, Object> values = new LinkedHashMap<>();

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

  public Integer getInteger(String name) {
    return TypeUtils.toInt(values.get(name));
  }

  public Integer[] getIntegerArray(String name) {
    return TypeUtils.toIntArray(values.get(name));
  }

  public Boolean getBoolean(String name) {
    return TypeUtils.toBool(values.get(name));
  }

  public Boolean[] getBooleanArray(String name) {
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
      if (col.getValue() instanceof Object[]) {
        builder
            .append(col.getKey())
            .append("='")
            .append(Arrays.toString((Object[]) col.getValue()))
            .append("' ");

      } else {
        builder.append(col.getKey()).append("='").append(col.getValue()).append("' ");
      }
    }
    builder.append(")");
    return builder.toString();
  }

  public Map<String, Object> getValueMap() {
    return this.values;
  }

  public Collection<String> getColumns() {
    return this.values.keySet();
  }

  public Object get(Type type, String name) throws MolgenisException {
    return get(type.getType(), name);
  }

  public <T> T get(Class<T> type, String name) throws MolgenisException {
    if (type == null) return null;

    switch (type.getSimpleName()) {
      case "String":
        return (T) getString(name);
      case "String[]":
        return (T) getStringArray(name);
      case "Integer":
        return (T) getInteger(name);
      case "Integer[]":
        return (T) getIntegerArray(name);
      case "Boolean":
        return (T) getBoolean(name);
      case "Boolean[]":
        return (T) getBooleanArray(name);
      case "Double":
        return (T) getDecimal(name);
      case "Double[]":
        return (T) getDecimalArray(name);
      case "LocalDate":
        return (T) getDate(name);
      case "LocalDate[]":
        return (T) getDateArray(name);
      case "LocalDateTime":
        return (T) getDateTime(name);
      case "LocalDateTime[]":
        return (T) getDateTimeArray(name);
      case "UUID":
        return (T) getUuid(name);
      case "UUID[]":
        return (T) getUuidArray(name);
      default:
        throw new MolgenisException(
            "Row.get(Class,name) not implemented for Class = " + type.getSimpleName());
    }
  }
}

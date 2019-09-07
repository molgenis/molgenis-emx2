package org.molgenis.emx2;

import org.molgenis.emx2.utils.TypeUtils;
import org.molgenis.emx2.utils.MolgenisException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Row {

  // option: use https://github.com/Devskiller/friendly-id instead of ugly uuid
  private Map<String, Object> values = new LinkedHashMap<>();

  public Row() {}

  public Row(Map<String, ?> values) {
    this();
    this.values.putAll(values);
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

  public Row setIntArray(String name, Integer[] value) {
    this.values.put(name, value);
    return this;
  }

  public Row setDecimal(String columnId, Double value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setDecimalArray(String name, Double[] value) {
    this.values.put(name, value);
    return this;
  }

  public Row setBool(String columnId, Boolean value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setBoolArray(String name, Boolean[] value) {
    this.values.put(name, value);
    return this;
  }

  public Row setDate(String columnId, LocalDate value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setDateArray(String columnId, LocalDate[] value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setDateTime(String columnId, LocalDateTime value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setDateTimeArray(String columnId, LocalDateTime[] value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setText(String columnId, String value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setTextArray(String columnId, String[] value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setUuid(String columnId, UUID value) {
    this.values.put(columnId, value);
    return this;
  }

  public Row setUuidArray(String columnId, UUID[] value) {
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

  public void set(Map<String, Object> values) {
    this.values.putAll(values);
  }

  public Row set(String name, Object value) {
    this.values.put(name, value);
    return this;
  }

  public Map<String, Object> getValueMap() {
    return this.values;
  }

  public Collection<String> getColumnNames() {
    return this.values.keySet();
  }

  public Object get(Type type, String name) throws MolgenisException {
    return get(name, type.getType());
  }

  public <T> T get(String name, Class<T> type) throws MolgenisException {
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
            "invalid_type",
            "Cannot cast column to java type",
            "Row.get(Class,name) not implemented for Class = " + type.getCanonicalName());
    }
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ROW(");
    for (Map.Entry<String, Object> col : values.entrySet()) {
      builder
          .append(col.getKey())
          .append("='")
          .append(TypeUtils.toString(col.getValue()))
          .append("' ");
    }
    builder.append(")");
    return builder.toString();
  }
}

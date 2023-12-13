package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.MG_DRAFT;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.jooq.JSONB;
import org.molgenis.emx2.utils.TypeUtils;

public class Row {
  private Map<String, Object> values = new LinkedHashMap<>();

  public Row(Row row) {
    for (Map.Entry<String, Object> e : row.getValueMap().entrySet()) {
      values.put(e.getKey(), e.getValue());
    }
  }

  public Row(Object... nameValuePairs) {
    if (nameValuePairs == null) return;
    if (nameValuePairs.length % 2 == 1) {
      throw new MolgenisException(
          "Row nameValue constructor should even number of parameters representing name-value pairs: Received "
              + nameValuePairs.length
              + " values");
    }
    for (int i = 0; i < nameValuePairs.length; i = i + 2) {
      if (!(nameValuePairs[i] instanceof String)) {
        throw new MolgenisException(
            "Row names should be not null string: found " + nameValuePairs[i]);
      }
      this.set((String) nameValuePairs[i], nameValuePairs[i + 1]);
    }
  }

  public static Row row() {
    return new Row();
  }

  public static Row row(Object... nameValuePairs) {
    return new Row(nameValuePairs);
  }

  public Row() {}

  public Row(Map<String, ?> values) {
    this();
    for (Map.Entry<String, ?> entry : values.entrySet()) {
      this.set(entry.getKey(), entry.getValue());
    }
  }

  public void clear(String name) {
    this.values.remove(name);
  }

  public UUID getUuid(String name) {
    return TypeUtils.toUuid(values.get(name));
  }

  public UUID[] getUuidArray(String name) {
    return TypeUtils.toUuidArray(values.get(name));
  }

  public String getString(String name, boolean emptyAsNull) {
    if (values.get(name) == null) {
      // if the key is present but no value, returning an empty string allows updating to an empty
      // value in the database (returning null will result in no update being performed)
      if (!emptyAsNull && values.containsKey(name)) {
        return "";
      }
      return null;
    }
    return TypeUtils.toString(values.get(name));
  }

  public String getString(String name) {
    return getString(name, true);
  }

  public String[] getStringArray(String name) {
    return getStringArray(name, true);
  }

  public String[] getStringArray(String name, boolean emptyAsNull) {
    if (values.get(name) == null) {
      // if the key is present but no value, returning an empty array allows updating to an empty
      // value in the database  (returning null will result in no update being performed)
      if (!emptyAsNull && values.containsKey(name)) {
        return new String[0];
      }
      return null;
    }
    return TypeUtils.toStringArray(values.get(name));
  }

  public Integer getInteger(String name) {
    return TypeUtils.toInt(values.get(name));
  }

  public Integer[] getIntegerArray(String name) {
    return TypeUtils.toIntArray(values.get(name));
  }

  public Long getLong(String name) {
    return TypeUtils.toLong(values.get(name));
  }

  public Long[] getLongArray(String name) {
    return TypeUtils.toLongArray(values.get(name));
  }

  public byte[] getBinary(String name) {
    return TypeUtils.toBinary(values.get(name));
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
    return TypeUtils.toText(values.get(name));
  }

  public String[] getTextArray(String name) {
    return TypeUtils.toTextArray(values.get(name));
  }

  public LocalDate getDate(String name) {
    return TypeUtils.toDate(values.get(name));
  }

  public LocalDate[] getDateArray(String name) {
    return TypeUtils.toDateArray(values.get(name));
  }

  public LocalDateTime getDateTime(String name) {
    return TypeUtils.toDateTime(values.get(name));
  }

  public LocalDateTime[] getDateTimeArray(String name) {
    return TypeUtils.toDateTimeArray(values.get(name));
  }

  public JSONB getJsonb(String name) {
    return TypeUtils.toJsonb(values.get(name));
  }

  public JSONB[] getJsonbArray(String name) {
    return TypeUtils.toJsonbArray(values.get(name));
  }

  public Row setString(String name, String value) {
    this.values.put(name, value);
    return this;
  }

  public Row setStringArray(String name, String... value) {
    this.values.put(name, value);
    return this;
  }

  public Row setInt(String name, Integer value) {
    this.values.put(name, value);
    return this;
  }

  public Row setLong(String name, Long value) {
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

  public Row setBinary(String name, File value) {
    this.setBinary(name, new BinaryFileWrapper(value));
    return this;
  }

  public Row setBinary(String name, BinaryFileWrapper value) {
    if (value == null) {
      // fix: this is needed to also empty all file metadata fields
      this.values.put(name, null);
      this.values.put(name + "_extension", null);
      this.values.put(name + "_mimetype", null);
      this.values.put(name + "_size", null);
      this.values.put(name + "_contents", null);
    } else {
      this.values.put(name, UUID.randomUUID().toString().replace("-", ""));
      this.values.put(name + "_extension", value.getExtension());
      this.values.put(name + "_mimetype", value.getMimeType());
      this.values.put(name + "_size", value.getSize());
      this.values.put(name + "_contents", value.getContents());
    }
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

  public List<Object> values(String... columns) {
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
    if (value instanceof File) {
      this.setBinary(name, (File) value);
    } else if (value instanceof BinaryFileWrapper) {
      this.setBinary(name, (BinaryFileWrapper) value);
    } else {
      this.values.put(name, value);
    }
    return this;
  }

  public Map<String, Object> getValueMap() {
    return this.values;
  }

  public Set<String> getColumnNames() {
    return this.values.keySet();
  }

  public Object get(String name, ColumnType columnType) {
    return get(name, columnType.getType());
  }

  public <T> T get(String name, Class<T> type) {
    if (type == null) return null;
    switch (type.getSimpleName()) {
      case "String":
        return (T) getString(name);
      case "String[]":
        return (T) getStringArray(name);
      case "JSONB":
        return (T) getJsonb(name);
      case "JSONB[]":
        return (T) getJsonbArray(name);
      case "Integer":
        return (T) getInteger(name);
      case "Integer[]":
        return (T) getIntegerArray(name);
      case "Long":
        return (T) getLong(name);
      case "Long[]":
        return (T) getLongArray(name);
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
      case "byte[]":
        return (T) getBinary(name);
      default:
        throw new MolgenisException(
            "Unknown type: Cannot cast column to java columnType. "
                + "Row.get(Class,name) not implemented for Class = "
                + type.getCanonicalName());
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

  public boolean containsName(String columnName) {
    return values.containsKey(columnName);
  }

  public boolean notNull(String columnName) {
    return values.get(columnName) != null && !values.get(columnName).toString().trim().equals("");
  }

  public boolean isNull(String columnName, ColumnType type) {
    return TypeUtils.isNull(values.get(columnName), type);
  }

  public Object get(Column column) {
    return get(column.getName(), column.getColumnType());
  }

  public boolean isDraft() {
    Boolean result = getBoolean(MG_DRAFT);
    return result != null && result == true;
  }

  public Row setDraft(boolean isDraft) {
    this.values.put(MG_DRAFT, isDraft);
    return this;
  }
}

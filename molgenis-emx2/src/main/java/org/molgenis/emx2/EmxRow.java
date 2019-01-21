package org.molgenis.emx2;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import com.fasterxml.uuid.Generators;

public class EmxRow {
  public EmxRow() {
    this(Generators.timeBasedGenerator().generate());
  }

  public static final String MOLGENISID = "molgenisid";
  protected Map<String, Object> valueMap = new LinkedHashMap<>();

  public EmxRow(Map<String, Object> valueMap) {
    this();
    this.valueMap.putAll(valueMap);
  }

  public EmxRow(UUID id) {
    setRowID(id);
  }

  public UUID getRowID() {
    return (UUID) valueMap.get(MOLGENISID);
  }

  public EmxRow setRowID(UUID id) {
    this.setRef(MOLGENISID, id);
    return this;
  }

  public UUID getUuid(String columnId) {
    return (UUID) this.valueMap.get(columnId);
  }

  public String getString(String name) {
    return (String) valueMap.get(name);
  }

  public Integer getInt(String name) {
    return (Integer) valueMap.get(name);
  }

  public Boolean getBool(String name) {
    return (Boolean) valueMap.get(name);
  }

  public Double getDecimal(String name) {
    return (Double) valueMap.get(name);
  }

  public String getText(String name) {
    return (String) valueMap.get(name);
  }

  public LocalDate getDate(String name) {
    if (valueMap.get(name) == null) return null;
    if (valueMap.get(name) instanceof Date) {
      return LocalDate.parse(valueMap.get(name).toString());
    } else if (valueMap.get(name) instanceof OffsetDateTime) {
      return ((OffsetDateTime) valueMap.get(name)).toLocalDate();
    } else {
      return (LocalDate) valueMap.get(name);
    }
  }

  public List<EmxRow> getMref(String name) {
    return (List<EmxRow>) valueMap.get(name);
  }

  public OffsetDateTime getDateTime(String name) {
    return (OffsetDateTime) valueMap.get(name);
  }

  public UUID getRef(String name) {
    return (UUID) valueMap.get(name);
  }

  public EmxRow setString(String name, String value) {
    valueMap.put(name, value);
    return this;
  }

  public EmxRow setInt(String name, Integer value) {
    valueMap.put(name, value);
    return this;
  }

  public EmxRow setRef(String name, EmxRow value) {
    valueMap.put(name, value.getRowID());
    return this;
  }

  public EmxRow setRef(String name, UUID value) {
    valueMap.put(name, value);
    return this;
  }

  public EmxRow setDecimal(String columnId, Double value) {
    valueMap.put(columnId, value);
    return this;
  }

  public EmxRow setBool(String columnId, Boolean value) {
    valueMap.put(columnId, value);
    return this;
  }

  public EmxRow setDate(String columnId, LocalDate value) {
    valueMap.put(columnId, value);
    return this;
  }

  public EmxRow setDateTime(String columnId, OffsetDateTime value) {
    valueMap.put(columnId, value);
    return this;
  }

  public EmxRow setText(String columnId, String value) {
    valueMap.put(columnId, value);
    return this;
  }

  public EmxRow setUuid(String columnId, UUID value) {
    valueMap.put(columnId, value);
    return this;
  }

  public List values(String... columns) {
    List<Object> result = new ArrayList<>();
    for (String name : columns) {
      result.add(valueMap.get(name));
    }
    return Collections.unmodifiableList(result);
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ROW(");
    for (Map.Entry<String, Object> col : valueMap.entrySet()) {
      builder.append(col.getKey()).append("='").append(col.getValue()).append("' ");
    }
    builder.append(")");
    return builder.toString();
  }

  public EmxRow setMref(String columnName, List<EmxRow> mrefList) {
    valueMap.put(columnName, mrefList);
    return this;
  }

  public Map<String, Object> getValueMap() {
    return valueMap;
  }

  public EmxRow setMref(String columnName, EmxRow... mrefs) {
    return this.setMref(columnName, Arrays.asList(mrefs));
  }

  public Collection<String> getColumns() {
    return valueMap.keySet();
  }
}

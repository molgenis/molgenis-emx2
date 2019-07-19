package org.molgenis.beans;

import com.fasterxml.uuid.Generators;
import org.molgenis.Row;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.molgenis.Database.RowLevelSecurity.MG_EDIT_ROLE;

public class RowBean implements Row {
  private Map<String, Object> values = new LinkedHashMap<>();
  private Map<String, List<UUID>> mrefs = new LinkedHashMap<>();

  public RowBean() { // to ensure we have nicely sorted record we have time based uuid
    this(Generators.timeBasedGenerator().generate());
  }

  public RowBean(Map<String, Object> values) {
    this.values.putAll(values);
  }

  public RowBean(UUID id) {
    setMolgenisid(id);
  }

  @Override
  public UUID getMolgenisid() {
    return (UUID) values.get(MOLGENISID);
  }

  @Override
  public Row setMolgenisid(UUID id) {
    this.setRef(MOLGENISID, id);
    return this;
  }

  @Override
  public UUID getUuid(String columnId) {
    return (UUID) this.values.get(columnId);
  }

  @Override
  public String getEnum(String name) {
    return (String) values.get(name);
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
  public void set(Map<String, Object> values) {
    this.values.putAll(values);
  }

  @Override
  public Row setString(String name, String value) {
    values.put(name, value);
    return this;
  }

  @Override
  public Row setEnum(String name, String value) {
    values.put(name, value);
    return this;
  }

  @Override
  public Row setInt(String name, Integer value) {
    values.put(name, value);
    return this;
  }

  @Override
  public Row setRef(String name, Row value) {
    values.put(name, value.getMolgenisid());
    return this;
  }

  @Override
  public Row setMref(String name, List<Row> values) {
    List<UUID> uuids = new ArrayList<>();
    for (Row r : values) uuids.add(r.getMolgenisid());
    mrefs.put(name, uuids);
    return this;
  }

  @Override
  public Row setMref(String colName, Row... values) {
    List<UUID> ids = new ArrayList<>();
    for (Row r : values) {
      ids.add(r.getMolgenisid());
    }
    mrefs.put(colName, ids);
    return this;
  }

  @Override
  public Row setRef(String name, UUID value) {
    values.put(name, value);
    return this;
  }

  @Override
  public Row setDecimal(String columnId, Double value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public Row setBool(String columnId, Boolean value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public Row setDate(String columnId, LocalDate value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public Row setDateTime(String columnId, OffsetDateTime value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public Row setText(String columnId, String value) {
    values.put(columnId, value);
    return this;
  }

  @Override
  public Row setUuid(String columnId, UUID value) {
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

  @Override
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

  @Override
  public Map<String, Object> getValueMap() {
    return values;
  }

  @Override
  public List<UUID> getMref(String colName) {
    return this.mrefs.get(colName);
  }

  @Override
  public Collection<String> getColumns() {
    return values.keySet();
  }

  @Override
  public Row setRowEditRole(String role) {
    return this.setString(MG_EDIT_ROLE.toString(), role);
  }
}

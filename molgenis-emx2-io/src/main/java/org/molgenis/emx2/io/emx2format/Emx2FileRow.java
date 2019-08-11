package org.molgenis.emx2.io.emx2format;

import org.molgenis.Row;

import static org.molgenis.emx2.io.emx2format.Emx2FileHeader.*;

public class Emx2FileRow {
  private String table;
  private String column;
  private String properties;
  private String description;

  public Emx2FileRow(String table, String column, String properties) {
    this.table = table == null ? "" : table.trim();
    this.column = column == null ? "" : column.trim();
    this.properties = properties == null ? "" : properties.trim();
  }

  public Emx2FileRow(Row row) {
    this(
        row.getString(TABLE.name().toLowerCase()),
        row.getString(COLUMN.name().toLowerCase()),
        row.getString(PROPERTIES.name().toLowerCase()),
        row.getString(DESCRIPTION.name().toLowerCase()));
  }

  public Emx2FileRow(String table, String column, String properties, String description) {
    this(table, column, properties);
    this.description = description == null ? "" : description.trim();
  }

  public String getTable() {
    return table;
  }

  public String getColumn() {
    return column;
  }

  public String getProperties() {
    return properties;
  }

  public String getDescription() {
    return description;
  }

  public Row toRow() {
    return new Row()
        .setString(TABLE.name().toLowerCase(), table)
        .setString(COLUMN.name().toLowerCase(), column)
        .setString(PROPERTIES.name().toLowerCase(), properties)
        .setString(DESCRIPTION.name().toLowerCase(), description);
  }

  public String toString() {
    return String.format("{table='%s', column='%s', definitions='%s'}", table, column, properties);
  }
}

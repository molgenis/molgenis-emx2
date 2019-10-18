package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.Row;

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
        row.getString(Emx2FileHeader.TABLE.name().toLowerCase()),
        row.getString(Emx2FileHeader.COLUMN.name().toLowerCase()),
        row.getString(Emx2FileHeader.PROPERTIES.name().toLowerCase()),
        row.getString(Emx2FileHeader.DESCRIPTION.name().toLowerCase()));
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
        .setString(Emx2FileHeader.TABLE.name().toLowerCase(), table)
        .setString(Emx2FileHeader.COLUMN.name().toLowerCase(), column)
        .setString(Emx2FileHeader.PROPERTIES.name().toLowerCase(), properties)
        .setString(Emx2FileHeader.DESCRIPTION.name().toLowerCase(), description);
  }

  public String toString() {
    return String.format("{table='%s', column='%s', definitions='%s'}", table, column, properties);
  }
}

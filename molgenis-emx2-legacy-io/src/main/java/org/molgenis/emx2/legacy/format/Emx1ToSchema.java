package org.molgenis.emx2.legacy.format;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.stores.RowStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.molgenis.emx2.ColumnType.*;

public class Emx1ToSchema {

  private Emx1ToSchema() {
    // hide constructor
  }

  public static SchemaMetadata convert(RowStore store) throws IOException {

    SchemaMetadata schema = new SchemaMetadata();

    if (store.containsTable("entities")) {
      for (Row row : store.read("entities")) {
        schema.createTableIfNotExists(row.getString("name"));
      }
    }
    List<Attribute> attributes = new ArrayList<>();
    if (store.containsTable("attributes")) {
      for (Row row : store.read("attributes")) {
        attributes.add(new Attribute(row));
      }
    }
    for (Attribute attribute : attributes) {

      // create the table
      TableMetadata table = schema.createTableIfNotExists(getTableName(attribute.getEntity()));

      // create the attribute
      ColumnType type = getColumnType(attribute.getDataType());
      Column column = new Column(table, attribute.getName(), type);
      // column.setRefTable(attribute.getRefEntity());
      column.setNullable(attribute.getNillable());
      column.setPrimaryKey(attribute.getIdAttribute());
      table.addColumn(column);
    }

    // update refEntity
    for (Attribute attribute : attributes) {
      if (attribute.getDataType().contains("ref")) {

        TableMetadata table = schema.getTableMetadata(getTableName(attribute.getEntity()));
        TableMetadata otherTable = schema.getTableMetadata(getTableName(attribute.getRefEntity()));

        table
            .getColumn(attribute.getName())
            .setReference(otherTable.getTableName(), otherTable.getPrimaryKey()[0]);
      }
    }

    return schema;
  }

  private static String getTableName(String fullName) {
    return fullName.contains("_") ? fullName.substring(fullName.lastIndexOf('_') + 1) : fullName;
  }

  public static ColumnType getColumnType(String dataType) {
    switch (dataType) {
      case "compound": // todo
      case "string":
      case "email":
      case "enum": // todo
      case "file": // todo
      case "hyperlink":
      case "one_to_many":
        return STRING; // todo
      case "text":
      case "html":
        return TEXT;
      case "int":
      case "long":
        return INT;
      case "decimal":
        return DECIMAL;
      case "bool":
        return BOOL;
      case "date":
        return DATE;
      case "datetime":
        return DATETIME;
      case "xref":
      case "categorical":
        return REF;
      case "mref":
      case "categorical_mref":
        return MREF;
      default:
        return STRING;
    }
  }
}

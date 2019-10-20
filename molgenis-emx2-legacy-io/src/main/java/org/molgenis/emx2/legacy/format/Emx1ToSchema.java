package org.molgenis.emx2.legacy.format;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.stores.RowStore;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.List;

import static org.molgenis.emx2.ColumnType.*;

public class Emx1ToSchema {

  private Emx1ToSchema() {
    // hide constructor
  }

  public static SchemaMetadata convert(RowStore store, String packagePrefix) {
    SchemaMetadata schema = new SchemaMetadata();

    int line = 2; // line 1 is header
    List<Entity> entities = new ArrayList<>();

    try {
      if (store.containsTable("entities")) {
        for (Row row : store.read("entities")) {
          entities.add(new Entity(row));
          line++;
        }
      }

      line = 2; // line 1 is header
      for (Entity entity : entities) {
        schema.createTableIfNotExists(entity.getName());
        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'entities' line " + line, me);
    }

    line = 2; // line 1 is header
    List<Attribute> attributes = new ArrayList<>();
    try {
      if (store.containsTable("attributes")) {
        for (Row row : store.read("attributes")) {
          attributes.add(new Attribute(row));
          line++;
        }
      }

      line = 2; // line 1 is header
      for (Attribute attribute : attributes) {

        // create the table
        TableMetadata table =
            schema.createTableIfNotExists(getTableName(attribute.getEntity(), packagePrefix));

        // create the attribute
        ColumnType type = getColumnType(attribute.getDataType());
        Column column = new Column(table, attribute.getName(), type);
        // column.setRefTable(attribute.getRefEntity());
        column.setNullable(attribute.getNillable());
        column.setPrimaryKey(attribute.getIdAttribute());
        table.addColumn(column);

        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'attributes' line " + line, me);
    }

    // update extends relationships
    line = 2; // line 1 is header
    try {
      for (Entity entity : entities) {
        if (entity.getExtends() != null) {
          schema
              .getTableMetadata(entity.getName())
              .inherits(getTableName(entity.getExtends(), packagePrefix));
        }
        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'entities' line " + line, me);
    }

    // update refEntity
    for (Attribute attribute : attributes) {
      if (attribute.getDataType().contains("ref")) {

        TableMetadata table =
            schema.getTableMetadata(getTableName(attribute.getEntity(), packagePrefix));
        TableMetadata otherTable =
            schema.getTableMetadata(getTableName(attribute.getRefEntity(), packagePrefix));

        if (otherTable.getPrimaryKey() == null || otherTable.getPrimaryKey().length == 0) {
          throw new MolgenisException(
              "missing_key",
              "Primary key is missing",
              "Table '" + otherTable.getTableName() + "' has no primary key defined");
        }
        table
            .getColumn(attribute.getName())
            .setReference(otherTable.getTableName(), otherTable.getPrimaryKey()[0]);
      }
    }

    return schema;
  }

  private static String getTableName(String fullName, String packagePrefix) {
    return fullName.replaceFirst(packagePrefix, "");
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

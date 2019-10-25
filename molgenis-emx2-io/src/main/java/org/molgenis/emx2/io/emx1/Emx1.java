package org.molgenis.emx2.io.emx1;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.stores.RowStore;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.ColumnType.*;

public class Emx1 {

  private Emx1() {
    // hide constructor
  }

  public static void upload(RowStore store, Schema targetSchema) {

    // parse metadata into emx1 schema
    SchemaMetadata emx1schema = new SchemaMetadata();
    Map<String, Entity> entities = loadTables(store, emx1schema);
    loadInheritanceRelations(emx1schema, entities);
    List<Attribute> attributes = loadColumns(store, entities, emx1schema);
    loadRefRelationships(emx1schema, entities, attributes);

    // load into target schema
    targetSchema.merge(emx1schema);
    for (Map.Entry<String, Entity> entity : entities.entrySet()) {
      if (store.containsTable(entity.getKey())) {
        targetSchema
            .getTable(entity.getValue().getName())
            .update(store.read(entity.getKey())); // actually upsert
      }
    }
  }

  private static void loadRefRelationships(
      SchemaMetadata schema, Map<String, Entity> entities, List<Attribute> attributes) {
    // update refEntity
    for (Attribute attribute : attributes) {
      if (attribute.getDataType().contains("ref")
          || attribute.getDataType().contains("categorical")) {

        TableMetadata table =
            schema.getTableMetadata(entities.get(attribute.getEntity()).getName());

        if (attribute.getRefEntity() == null) {
          throw new MolgenisException(
              "missing_refentity",
              "Refentity is missing for attribute '",
              "Adding reference '"
                  + attribute.getEntity()
                  + "'.'"
                  + attribute.getName()
                  + "' failed. RefEntity was missing");
        }
        table
            .getColumn(attribute.getName())
            .setReference(entities.get(attribute.getRefEntity()).getName(), null);
      }
    }
  }

  private static void loadInheritanceRelations(
      SchemaMetadata schema, Map<String, Entity> entities) {
    int line = 2; // line 1 is header
    try {
      for (Entity entity : entities.values()) {
        if (entity.getExtends() != null) {
          schema
              .getTableMetadata(entity.getName())
              .inherits(entities.get(entity.getExtends()).getName());
        }
        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'entities' line " + line, me);
    }
  }

  private static List<Attribute> loadColumns(
      RowStore store, Map<String, Entity> entities, SchemaMetadata schema) {
    int line = 2; // line 1 is header
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
        TableMetadata table = schema.createTable(entities.get(attribute.getEntity()).getName());

        // create the attribute
        ColumnType type = getColumnType(attribute.getDataType());
        Column column = table.addColumn(attribute.getName(), type);
        column.setNullable(attribute.getNillable());
        column.setPrimaryKey(attribute.getIdAttribute());
        table.addColumn(column);

        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'attributes' line " + line, me);
    }
    return attributes;
  }

  private static Map<String, Entity> loadTables(RowStore store, SchemaMetadata schema) {
    Map<String, Entity> entities = new LinkedHashMap<>();
    int line = 2; // line 1 is header

    try {
      if (store.containsTable("entities")) {
        for (Row row : store.read("entities")) {
          Entity e = new Entity(row);
          entities.put(e.getPackageName() + "_" + e.getName(), e);
          line++;
        }
      }

      line = 2; // line 1 is header
      for (Entity entity : entities.values()) {
        schema.createTable(entity.getName());
        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'entities' line " + line, me);
    }
    return entities;
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
        return REF_ARRAY; // todo: or should we use mref? but that is only in case of two sided
        // references ATM
      default:
        return STRING;
    }
  }
}

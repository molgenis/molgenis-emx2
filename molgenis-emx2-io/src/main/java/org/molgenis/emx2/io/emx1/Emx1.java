package org.molgenis.emx2.io.emx1;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.rowstore.TableStore;
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

  public static void uploadFromStoreToSchema(TableStore store, Schema targetSchema) {

    // parse metadata into emx1 schema
    SchemaMetadata emx1schema = new SchemaMetadata();
    Map<String, Emx1Entity> entities = loadTables(store, emx1schema);
    loadInheritanceRelations(emx1schema, entities);
    List<Emx1Attribute> attributes = loadColumns(store, entities, emx1schema);
    loadRefRelationships(emx1schema, entities, attributes);

    // load into target schema
    targetSchema.merge(emx1schema);
    for (Map.Entry<String, Emx1Entity> entity : entities.entrySet()) {
      if (store.containsTable(entity.getKey())) {
        targetSchema
            .getTable(entity.getValue().getName())
            .update(store.readTable(entity.getKey())); // actually upsert
      }
    }
  }

  private static void loadRefRelationships(
      SchemaMetadata schema, Map<String, Emx1Entity> entities, List<Emx1Attribute> attributes) {
    // update refEntity
    for (Emx1Attribute attribute : attributes) {
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
      SchemaMetadata schema, Map<String, Emx1Entity> entities) {
    int line = 2; // line 1 is header
    try {
      for (Emx1Entity entity : entities.values()) {
        if (entity.getExtends() != null) {
          schema
              .getTableMetadata(entity.getName())
              .setInherit(entities.get(entity.getExtends()).getName());
        }
        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          me.getType(), me.getTitle(), me.getDetail() + ". See 'entities' line " + line, me);
    }
  }

  private static List<Emx1Attribute> loadColumns(
      TableStore store, Map<String, Emx1Entity> entities, SchemaMetadata schema) {
    int line = 2; // line 1 is header
    List<Emx1Attribute> attributes = new ArrayList<>();
    try {
      if (store.containsTable("attributes")) {
        for (Row row : store.readTable("attributes")) {
          attributes.add(new Emx1Attribute(row));
          line++;
        }
      }

      line = 2; // line 1 is header
      for (Emx1Attribute attribute : attributes) {

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

  private static Map<String, Emx1Entity> loadTables(TableStore store, SchemaMetadata schema) {
    Map<String, Emx1Entity> entities = new LinkedHashMap<>();
    int line = 2; // line 1 is header

    try {
      if (store.containsTable("entities")) {
        for (Row row : store.readTable("entities")) {
          Emx1Entity e = new Emx1Entity(row);
          if (e.getPackageName() != null) {
            entities.put(e.getPackageName() + "_" + e.getName(), e);
          } else {
            entities.put(e.getName(), e);
          }
          line++;
        }
      }

      line = 2; // line 1 is header
      for (Emx1Entity entity : entities.values()) {
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

  private static ColumnType getColumnType(String dataType) {
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

package org.molgenis.emx2.io.emx1;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Emx1 {
  static final String EMX_1_IMPORT_FAILED = "EMX1 import failed: ";
  public static final String ONETOMANY = "onetomany";
  private static Logger logger = LoggerFactory.getLogger(Emx1.class);

  private Emx1() {
    // hide constructor
  }

  public static void uploadFromStoreToSchema(TableStore store, Schema targetSchema) {
    long start = System.currentTimeMillis();

    // parse metadata into emx1 schema
    SchemaMetadata emx1schema = new SchemaMetadata();
    Map<String, Emx1Entity> entities = loadTables(store, emx1schema);
    loadInheritanceRelations(emx1schema, entities);
    List<Emx1Attribute> attributes = loadColumns(store, entities, emx1schema);
    loadRefRelationships(emx1schema, entities, attributes);

    // load into target schema
    targetSchema.migrate(emx1schema);

    // revert map
    Map<String, String> tableToSheet = new LinkedHashMap<>();
    for (Map.Entry<String, Emx1Entity> entry : entities.entrySet()) {
      tableToSheet.put(entry.getValue().getName(), entry.getKey());
    }

    // load the tables
    for (TableMetadata table : emx1schema.getTables()) {
      if (store.containsTable(tableToSheet.get(table.getTableName()))) {
        targetSchema
            .getTable(table.getTableName())
            .save(store.readTable(tableToSheet.get(table.getTableName()), null)); // actually upsert
      }
    }

    if (logger.isInfoEnabled()) {
      logger.info("import completed in {}ms", (System.currentTimeMillis() - start));
    }
  }

  private static void loadRefRelationships(
      SchemaMetadata schema, Map<String, Emx1Entity> entities, List<Emx1Attribute> attributes) {
    // update refEntity
    for (Emx1Attribute attribute : attributes) {
      if (attribute.getDataType().contains("ref")
          || attribute.getDataType().contains("categorical")
          || attribute.getDataType().contains(ONETOMANY)) {

        if (attribute.getRefEntity() == null) {
          throw new MolgenisException(
              EMX_1_IMPORT_FAILED
                  + "Adding reference '"
                  + attribute.getEntity()
                  + "'.'"
                  + attribute.getName()
                  + "' failed. RefEntity was missing");
        }

        TableMetadata table =
            schema.getTableMetadata(getTableName(entities, attribute.getEntity()));

        String refTableName = getTableName(entities, attribute.getRefEntity());

        Column c = table.getColumn(attribute.getName()).setRefTable(refTableName);
        if (attribute.getDataType().contains(ONETOMANY) && attribute.getRefBack() == null) {
          throw new MolgenisException(
              "refBack missing for attribute " + attribute.getEntity() + "." + attribute.getName());
        }
        if (attribute.getRefBack() != null) {
          c.setRefBack(attribute.getRefBack());
        }
        table.alterColumn(c);
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
          EMX_1_IMPORT_FAILED + me.getMessage() + "See 'entities' line " + line, me);
    }
  }

  private static List<Emx1Attribute> loadColumns(
      TableStore store, Map<String, Emx1Entity> entities, SchemaMetadata schema) {
    int line = 2; // line 1 is header
    List<Emx1Attribute> attributes = new ArrayList<>();
    try {
      if (store.containsTable("attributes")) {
        for (Row row : store.readTable("attributes", null)) {
          attributes.add(new Emx1Attribute(row));
          line++;
        }
      }

      line = 2; // line 1 is header
      for (Emx1Attribute attribute : attributes) {

        // create the table, if needed
        String entityName = attribute.getEntity();
        String tableName = getTableName(entities, entityName);
        TableMetadata table = schema.getTableMetadata(tableName);
        if (table == null) {
          table = schema.create(table(tableName));
        }

        // create the attribute
        ColumnType type = getColumnType(attribute.getDataType());
        Column column =
            column(attribute.getName()).setType(type).setRequired(!attribute.getNillable());

        // pkey
        if (attribute.getIdAttribute()) {
          column.setKey(1);
        }
        table.add(column);

        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          EMX_1_IMPORT_FAILED + me.getMessage() + ". See 'attributes' line " + line, me);
    }
    return attributes;
  }

  private static String getTableName(Map<String, Emx1Entity> entities, String entityName) {
    return entities.get(entityName) != null ? entities.get(entityName).getName() : entityName;
  }

  private static Map<String, Emx1Entity> loadTables(TableStore store, SchemaMetadata schema) {
    Map<String, Emx1Entity> entities = new LinkedHashMap<>();
    int line = 2; // line 1 is header

    try {
      if (store.containsTable("entities")) {
        for (Row row : store.readTable("entities", null)) {
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
        schema.create(table(entity.getName()));
        line++;
      }
    } catch (MolgenisException me) {
      throw new MolgenisException(
          EMX_1_IMPORT_FAILED + me.getMessage() + ". See 'entities' line " + line, me);
    }
    return entities;
  }

  private static ColumnType getColumnType(String dataType) {
    switch (dataType) {
      case "compound": // unsupported, functionally equivalent to heading
        return HEADING;
      case "hyperlink":
        return HYPERLINK;
      case "email":
        return EMAIL;
      case "file":
        return FILE;
      case "text":
      case "html":
        return TEXT;
      case "int":
        return INT;
      case "long":
        return LONG;
      case "decimal":
        return DECIMAL;
      case "bool":
        return BOOL;
      case "date":
        return DATE;
      case "datetime":
        return DATETIME;
      case "xref":
      case "categorical": // tbd: do we also want categorical?
        return REF;
      case ONETOMANY:
        return REFBACK;
      case "mref":
      case "categorical_mref":
        return REF_ARRAY;
      default:
        return STRING; // string, enum, others will default to string
    }
  }

  public static List<Row> getEmx1Entities(SchemaMetadata schema) {
    List<Row> result = new ArrayList<>();
    for (TableMetadata table : schema.getTables()) {
      Emx1Entity e = new Emx1Entity(table);
      result.add(e.toRow());
    }
    return result;
  }

  public static List<Row> getEmx1Attributes(SchemaMetadata schema) {
    List<Row> rows = new ArrayList<>();
    for (TableMetadata table : schema.getTables()) {
      for (Column c : table.getLocalColumns()) {
        Emx1Attribute a = new Emx1Attribute(c);
        rows.add(a.toRow());
      }
    }
    return rows;
  }
}

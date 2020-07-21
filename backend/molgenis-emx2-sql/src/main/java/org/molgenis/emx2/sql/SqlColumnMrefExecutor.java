package org.molgenis.emx2.sql;

import org.jooq.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.utils.TypeUtils.getNonArrayType;
import static org.molgenis.emx2.utils.TypeUtils.toJooqType;

public class SqlColumnMrefExecutor {

  public static void createMrefConstraints(DSLContext jooq, Column column) {
    createArrayColumn(jooq, column);
    createJoinTable(jooq, column);
    createUpdateTrigger(jooq, column);
  }

  private static void createArrayColumn(DSLContext jooq, Column column) {}

  private static void createUpdateTrigger(DSLContext jooq, Column column) {

    //  parameters
    String schemaName = column.getTable().getSchema().getName();
    String insertOrUpdateTrigger =
        column.getTable().getTableName() + "_" + column.getName() + "_TRIGGER";

    // trigger to insert all missing

    // if update, also trigger to delete all not in the array(s) any more
    jooq.execute(
        "CREATE OR REPLACE FUNCTION {0}() RETURNS trigger AS $BODY$ BEGIN"
            // delete all from previous
            + "\n\tIF TG_OP='UPDATE' THEN DELETE FROM {1} WHERE {2}; END IF;"
            // add all unique references that are in the ref_array(s)
            // NEW.ref_array column(s) = NULL, unless this is INSERT and we can expect ON CONFLICT
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {4} WHERE {5}) THEN "
            + "\n\t\tINSERT INTO {1} (SELECT * FROM ({3}) foo);"
            + "\n\t\t{6}; END IF;"
            + "\n\tRETURN NEW;END;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        // 0 name of the trigger
        name(schemaName, insertOrUpdateTrigger),
        // 1 name of the join table
        name(schemaName, getJoinTableName(column)),
        // 2 OLD.id = id
        keyword(whereOldIdEqualsId(column)),
        // 3 subquery to check array contents against jointable content
        keyword(subQuery(column)),
        // 4 self tablename
        name(schemaName, column.getTable().getTableName()),
        // 5 filter on 'NEW.key = key' (for each key in composite key)
        keyword(primaryKeyFilter(column)),
        // 6 set all 'NEW.ref_array = NULL'
        keyword(setRefArrayNull(column)));

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2} "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(insertOrUpdateTrigger),
        keyword(refColumnNames(column)),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, insertOrUpdateTrigger));
  }

  private static String whereOldIdEqualsId(Column column) {
    List<String> items = new ArrayList<>();
    for (String pkey : column.getTable().getPrimaryKeys()) {
      String name = name(pkey).toString();
      items.add("OLD." + name + " = " + name);
    }
    return String.join(" AND ", items);
  }

  private static String refColumnNames(Column column) {
    List<String> items = new ArrayList<>();
    for (Reference ref : column.getRefColumns()) {
      items.add(name(ref.getName()).toString());
    }
    return String.join(",", items);
  }

  // NEW.{ref_array1} = NULL [, NEW.{ref_array2} = NULL]
  private static String setRefArrayNull(Column column) {
    List<String> items = new ArrayList<>();
    for (Reference ref : column.getRefColumns()) {
      items.add("NEW." + name(ref.getName()) + " = NULL");
    }
    return String.join(";", items);
  }

  // key1 = NEW.key1 [, key2 = NEW.key2]
  private static String primaryKeyFilter(Column column) {
    List<String> items = new ArrayList<>();
    for (String pkey : column.getTable().getPrimaryKeys()) {
      String name = name(pkey).toString();
      items.add("NEW." + name + " = " + name);
    }
    return String.join(" AND ", items);
  }

  // "SELECT ({NEW.{keyfield} as {keyfield}}) AS self, UNNEST({refFields}) AS other({refFields}
  private static String subQuery(Column column) {
    StringBuffer result = new StringBuffer();

    // SELECT ({NEW.{keyfield} as {keyfield}}) AS self
    List<String> items = new ArrayList<>();
    for (String pkey : column.getTable().getPrimaryKeys()) {
      items.add("NEW." + name(pkey) + " AS " + name(pkey));
    }
    result.append("SELECT " + String.join(",", items) + ",  other.* FROM ");

    // UNNEST({refFields-name}) AS other({refFields-name}
    items = new ArrayList<>();
    List<String> items2 = new ArrayList<>();
    for (Reference ref : column.getRefColumns()) {
      Name name = name(ref.getName());
      items.add("NEW." + name);
      items2.add(name(name).toString());
    }
    String refFields = String.join(",", items);
    String asNames = String.join(",", items2);
    result.append("UNNEST(" + refFields + ") as other(" + asNames + ")");
    return result.toString();
  }

  public static void dropMrefConstraints(DSLContext jooq, Column column) {
    Name tableName = name(column.getTable().getSchemaName(), getJoinTableName(column));
    jooq.dropTable(tableName);
  }

  private static String getJoinTableName(Column column) {
    return column.getTableName() + "-" + column.getName();
  }

  private static void createJoinTable(DSLContext jooq, Column column) {

    // Define the parameters
    Name tableName = name(column.getTable().getSchemaName(), getJoinTableName(column));
    Name thisTable = name(column.getTable().getSchemaName(), column.getTableName());
    List<Field> selfFields = new ArrayList<>();
    List<Name> selfKeyFields = new ArrayList<>();
    Name otherTable = name(column.getTable().getSchemaName(), column.getRefTableName());
    List<Field> otherFields = new ArrayList<>();
    List<Name> otherFkeyFields = new ArrayList<>();

    // define the columns
    if (column.getTable().getPrimaryKeyColumns().size() == 0) {
      throw new MolgenisException(
          "Cannot create ref_array '" + column.getName() + "'", "Primary key not set");
    }
    for (Field thisKey : column.getTable().getPrimaryKeyFields()) {
      selfKeyFields.add(thisKey.getQualifiedName());
      thisKey = field(name(thisKey.getName()), thisKey.getDataType());
      selfFields.add(thisKey);
    }
    for (Reference ref : column.getRefColumns()) {
      otherFkeyFields.add(name(ref.getTo()));
      otherFields.add(field(name(ref.getName()), toJooqType(getNonArrayType(ref.getColumnType()))));
    }

    // all fields
    Collection<Field> fields = new ArrayList<>();
    fields.addAll(selfFields);
    fields.addAll(otherFields);
    Field[] fieldsArray = fields.toArray(new Field[fields.size()]);

    // create relationships
    Constraint primaryKey = constraint().primaryKey(fieldsArray);
    Name selfName = name(getJoinTableName(column) + "-self");
    ConstraintForeignKeyOnStep selfFkeyConstraint =
        constraint(selfName)
            .foreignKey(selfFields.toArray(new Field[selfFields.size()]))
            .references(thisTable, selfKeyFields.toArray(new Name[selfKeyFields.size()]))
            .onUpdateCascade()
            .onDeleteCascade(); // one end is master
    Name otherName = name(getJoinTableName(column) + "-other");
    ConstraintForeignKeyOnStep otherFkeyConstraint =
        constraint(otherName)
            .foreignKey(otherFields.toArray(new Field[otherFields.size()]))
            .references(otherTable, otherFkeyFields.toArray(new Name[otherFkeyFields.size()]))
            .onUpdateCascade();

    // execute
    jooq.createTable(tableName)
        .columns(fieldsArray)
        .constraint(primaryKey)
        .constraint(selfFkeyConstraint)
        .constraint(otherFkeyConstraint)
        .execute();

    jooq.execute(
        "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE", tableName, selfName);
    jooq.execute(
        "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE",
        tableName, otherName);
  }
}

package org.molgenis.emx2.sql;

import org.jooq.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.utils.TypeUtils.getNonArrayType;
import static org.molgenis.emx2.utils.TypeUtils.toJooqType;

public class SqlColumnMrefExecutor {

  private SqlColumnMrefExecutor() {
    // hide constructor
  }

  public static void createMrefConstraints(DSLContext jooq, Column column) {
    createJoinTable(jooq, column);
    createInsertUpdateTrigger(jooq, column);
  }

  private static void createInsertUpdateTrigger(DSLContext jooq, Column column) {

    //  parameters
    String schemaName = column.getTable().getSchema().getName();
    String insertOrUpdateTrigger = getInsertOrUpdateTriggerName(column);

    // trigger to insert all missing

    // if update, also trigger to delete all not in the array(s) any more
    jooq.execute(
        "CREATE OR REPLACE FUNCTION {0}() RETURNS trigger AS $BODY$ BEGIN"
            // delete all from previous
            + "\n\tIF TG_OP='UPDATE' THEN DELETE FROM {1} WHERE {2}; END IF;"
            // add all unique references that are in the ref_array(s)
            // NEW.ref_array column(s) = NULL, unless this is INSERT and we can expect ON CONFLICT
            + "\n\tINSERT INTO {1} ({3}) ON CONFLICT DO NOTHING;"
            + "\n\tRETURN NULL;"
            + "\n\tEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        // 0 name of the trigger
        name(schemaName, insertOrUpdateTrigger),
        // 1 name of the join table
        name(schemaName, getJoinTableName(column)),
        // 2 id IN (oldtab.ID)
        keyword(whereIdInNewtabId(column)),
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
            + "\n\tAFTER INSERT ON {2} "
            + "\n\tREFERENCING NEW TABLE AS newtab "
            + "\n\tFOR EACH STATEMENT EXECUTE PROCEDURE {3}()",
        name(insertOrUpdateTrigger + "_ins"),
        keyword(refColumnNames(column)),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, insertOrUpdateTrigger));

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER UPDATE ON {2} "
            + "\n\tREFERENCING NEW TABLE AS newtab "
            + "\n\tFOR EACH STATEMENT EXECUTE PROCEDURE {3}()",
        name(insertOrUpdateTrigger + "_upd"),
        keyword(refColumnNames(column)),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, insertOrUpdateTrigger));
  }

  private static String getInsertOrUpdateTriggerName(Column... column) {
    return column[0].getTable().getTableName()
        + "_"
        + List.of(column).stream().map(Column::getName).collect(Collectors.joining(","))
        + "_TRIGGER";
  }

  private static String whereIdInNewtabId(Column... column) {
    List<String> items = new ArrayList<>();
    for (String pkey : column[0].getTable().getPrimaryKeys()) {
      String name = name(pkey).toString();
      items.add(name + " IN (SELECT " + name + " FROM newtab)");
    }
    return String.join(" AND ", items);
  }

  private static String refColumnNames(Column... column) {
    return List.of(column).stream()
        .map(c -> name(c.getName()).toString())
        .collect(Collectors.joining(","));
  }

  // NEW.{ref_array1} = NULL [, NEW.{ref_array2} = NULL]
  private static String setRefArrayNull(Column... column) {
    List<String> items = new ArrayList<>();
    for (Column ref : column) {
      items.add("newtab." + name(ref.getName()) + " = NULL");
    }
    return String.join(";", items);
  }

  // key1 = NEW.key1 [, key2 = NEW.key2]
  private static String primaryKeyFilter(Column... column) {
    List<String> items = new ArrayList<>();
    for (String pkey : column[0].getTable().getPrimaryKeys()) {
      String name = name(pkey).toString();
      items.add("newtab." + name + " = " + name);
    }
    return String.join(" AND ", items);
  }

  private static String subQuery(Column column) {
    StringBuilder result = new StringBuilder();

    // SELECT ({NEW.{keyfield} as {keyfield}}) AS self
    List<String> items = new ArrayList<>();
    for (String pkey : column.getTable().getPrimaryKeys()) {
      items.add(name(pkey).toString());
    }
    result.append("SELECT " + String.join(",", items) + ", ");

    // UNNEST({refFields-name}) AS other({refFields-name}
    items = new ArrayList<>();
    for (Reference ref : column.getReferences()) {
      Name name = name(ref.getName());
      items.add("UNNEST(newtab." + name + ")");
    }
    result.append(String.join(",", items));
    result.append(" FROM newtab");
    return result.toString();
  }

  public static void dropMrefConstraints(DSLContext jooq, Column... column) {
    Name tableName = name(column[0].getTable().getSchemaName(), getJoinTableName(column));
    jooq.dropTable(tableName);
  }

  private static String getJoinTableName(Column... column) {
    return column[0].getTableName()
        + "-"
        + List.of(column).stream().map(Column::getName).collect(Collectors.joining(","));
  }

  private static void createJoinTable(DSLContext jooq, Column column) {

    // Define the parameters
    Name tableName = name(column.getSchemaName(), getJoinTableName(column));
    Name thisTable = name(column.getSchemaName(), column.getTableName());
    List<Field> selfFields = new ArrayList<>();
    List<Name> selfKeyFields = new ArrayList<>();
    Name otherTable = name(column.getSchemaName(), column.getRefTableName());
    List<Field> otherFields = new ArrayList<>();
    List<Name> otherFkeyFields = new ArrayList<>();

    // define the columns
    if (column.getTable().getPrimaryKeyColumns().isEmpty()) {
      throw new MolgenisException(
          "Cannot create ref_array '" + column.getName() + "': Primary key not set");
    }
    for (Field thisKey : column.getTable().getPrimaryKeyFields()) {
      selfKeyFields.add(thisKey.getQualifiedName());
      thisKey = field(name(thisKey.getName()), thisKey.getDataType());
      selfFields.add(thisKey);
    }
    for (Reference ref : column.getReferences()) {
      otherFkeyFields.add(name(ref.getRefTo()));
      otherFields.add(
          field(name(ref.getName()), toJooqType(getNonArrayType(ref.getPrimitiveType()))));
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
    try (CreateTableColumnStep t = jooq.createTable(tableName)) {
      t.columns(fieldsArray)
          .constraint(primaryKey)
          .constraint(selfFkeyConstraint)
          .constraint(otherFkeyConstraint)
          .execute();
    }

    jooq.execute(
        "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE", tableName, selfName);
    jooq.execute(
        "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE",
        tableName, otherName);
  }
}

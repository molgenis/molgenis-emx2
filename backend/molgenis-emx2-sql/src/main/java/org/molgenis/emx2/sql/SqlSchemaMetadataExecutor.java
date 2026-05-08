package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeDropTable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DDLQuery;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.molgenis.emx2.*;

class SqlSchemaMetadataExecutor {

  private SqlSchemaMetadataExecutor() {
    // hide
  }

  static void executeCreateSchema(SqlDatabase db, SchemaMetadata schema) {
    DDLQuery step = db.getJooq().createSchema(schema.getName());
    step.execute();

    String schemaName = schema.getName();
    String exists = getRolePrefix(schemaName) + EXISTS;
    String range = getRolePrefix(schemaName) + RANGE;
    String aggregator = getRolePrefix(schemaName) + AGGREGATOR;
    String count = getRolePrefix(schemaName) + COUNT;
    String viewer = getRolePrefix(schemaName) + VIEWER;
    String editor = getRolePrefix(schemaName) + EDITOR;
    String manager = getRolePrefix(schemaName) + MANAGER;
    String owner = getRolePrefix(schemaName) + OWNER;

    db.addRole(exists);
    db.addRole(range);
    db.addRole(aggregator);
    db.addRole(count);
    db.addRole(viewer);
    db.addRole(editor);
    db.addRole(manager);
    db.addRole(owner);

    // grant range role also exists role
    db.getJooq().execute("GRANT {0} TO {1}", name(exists), name(range));
    // grant aggregator role also exists role
    db.getJooq().execute("GRANT {0} TO {1}", name(range), name(aggregator));
    // make counter also aggregator
    db.getJooq().execute("GRANT {0} TO {1}", name(aggregator), name(count));
    // make viewer also counter
    db.getJooq().execute("GRANT {0} TO {1}", name(count), name(viewer));
    // make editor also viewer
    db.getJooq().execute("GRANT {0} TO {1}", name(viewer), name(editor));

    db.getJooq()
        .execute(
            "GRANT {0},{1},{2},{3},{4},{5} TO {6} WITH ADMIN OPTION",
            name(exists),
            name(range),
            name(aggregator),
            name(count),
            name(viewer),
            name(editor),
            name(manager));

    db.getJooq()
        .execute(
            "GRANT {0},{1},{2},{3},{4},{5},{6} TO {7} WITH ADMIN OPTION",
            name(exists),
            name(range),
            name(aggregator),
            name(count),
            name(viewer),
            name(editor),
            name(manager),
            name(owner));

    String currentUser = db.getJooq().fetchOne("SELECT current_user").get(0, String.class);
    String sessionUser = db.getJooq().fetchOne("SELECT session_user").get(0, String.class);

    // make current user the owner
    if (!sessionUser.equals(currentUser)) {
      db.getJooq().execute("GRANT {0} TO {1}", name(manager), name(currentUser));
    }

    // make admin owner
    db.getJooq().execute("GRANT {0} TO {1}", name(manager), name(sessionUser));

    // grant the permissions
    db.getJooq().execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schema.getName()), name(exists));
    // grant the permissions
    db.getJooq().execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schema.getName()), name(manager));

    MetadataUtils.saveSchemaMetadata(db.getJooq(), schema);
  }

  static String getRolePrefix(String name) {
    return Constants.MG_ROLE_PREFIX + name + "/";
  }

  static List<String> getInheritedRoleForUser(DSLContext jooq, String schemaName, String user) {
    String roleFilter = getRolePrefix(schemaName);
    List<Record> roles =
        jooq.fetch(
            "SELECT a.oid, a.rolname FROM pg_roles a WHERE pg_has_role({0}, a.oid, 'member') AND a.rolname LIKE {1}",
            Constants.MG_USER_PREFIX + user, roleFilter + "%");
    return roles.stream()
        .map(r -> r.get("rolname", String.class).substring(roleFilter.length()))
        .collect(Collectors.toList());
  }

  static void executeDropSchema(SqlDatabase db, String schemaName) {
    try {
      Schema schema = db.getSchema(schemaName);
      // reload because we must have latest state
      ((SqlSchemaMetadata) schema.getMetadata()).reload();

      // remove changelog triggers + table
      ChangeLogExecutor.disableChangeLog(db, schema.getMetadata());
      ChangeLogExecutor.executeDropChangeLogTableForSchema(db, schema);

      // remove foreign keys first to prevent foreign key errors in the schema
      db.getSchema(schemaName)
          .getTablesSorted()
          .forEach(
              table -> {
                table
                    .getMetadata()
                    .getColumns()
                    .forEach(
                        column -> {
                          if (column.isReference() && !column.isPrimaryKey()) {
                            table.getMetadata().dropColumn(column.getName());
                          }
                        });
              });

      // remove tables individually to trigger foreign key error if appropriate
      List<Table> tables = db.getSchema(schemaName).getTablesSorted();
      Collections.reverse(tables);
      tables.forEach(table -> executeDropTable(db.getJooq(), table.getMetadata()));

      // drop schema
      db.getJooq().dropSchema(name(schemaName)).execute();

      for (String role : db.getRoleManager().getRoleNames(schemaName)) {
        db.getJooq().execute("DROP ROLE IF EXISTS {0}", name(getRolePrefix(schemaName) + role));
      }
      MetadataUtils.deleteSchema(db.getJooq(), schemaName);
    } catch (Exception e) {
      throw new SqlMolgenisException("Drop schema failed", e);
    }
  }
}

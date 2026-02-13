package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.MetadataUtils.*;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeDropTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;

class SqlSchemaMetadataExecutor {

  private SqlSchemaMetadataExecutor() {
    // hide
  }

  static void executeCreateSchema(SqlDatabase db, SchemaMetadata schema) {
    db.getJooq().createSchema(schema.getName()).execute();

    MetadataUtils.saveSchemaMetadata(db.getJooq(), schema);

    // Create default role hierarchy (replaces trigger-based create_or_update_schema_groups)
    SqlPermissionExecutor.createSchemaGroups(db.getJooq(), schema.getName());
  }

  static void executeAddMembers(DSLContext jooq, Schema schema, Member member) {
    List<String> currentRoles = schema.getRoles();

    if (!currentRoles.contains(member.getRole())) {
      throw new MolgenisException(
          "Add member(s) failed: Role '"
              + member.getRole()
              + " doesn't exist in schema '"
              + schema.getMetadata().getName()
              + "'. Existing roles are: "
              + currentRoles);
    }

    if (!schema.getDatabase().hasUser(member.getUser())) {
      schema.getDatabase().addUser(member.getUser());
    }

    // Add user to group and grant PostgreSQL role (replaces trigger-based approach)
    String groupName = schema.getName() + "/" + member.getRole();
    SqlPermissionExecutor.addUserToGroup(jooq, groupName, member.getUser());
  }

  static String getRolePrefix(String name) {
    return Constants.MG_ROLE_PREFIX + name + "/";
  }

  static List<String> getInheritedRoleForUser(DSLContext jooq, String schemaName, String user) {
    String roleFilter = getRolePrefix(schemaName);
    List<Record> roles =
        jooq.fetch(
            """
                    SELECT gm.group_name as role, gm.users as members\s
                        FROM "MOLGENIS".group_metadata gm
                        JOIN "MOLGENIS".group_permissions gp ON gm.group_name = gp.group_name
                    WHERE gp.table_schema = {0}
                      AND {1} = ANY(gm.users);
                    """,
            schemaName, user);
    return roles.stream()
        .map(r -> r.get("role", String.class).substring(roleFilter.length()))
        .toList();
  }

  static List<Member> executeGetMembers(DSLContext jooq, SchemaMetadata schema) {
    List<Member> members = new ArrayList<>();

    List<Record> result =
        jooq.fetch(
            """
                SELECT gm.group_name as role, gm.users as members FROM "MOLGENIS".group_metadata gm
                        JOIN "MOLGENIS".group_permissions gp ON gm.group_name = gp.group_name
                        WHERE table_schema = {0} AND cardinality(gm.users) > 0;
                """,
            schema.getName());
    for (Record r : result) {
      List<String> membersResult = r.getValue("members", List.class);
      String roleName = r.getValue("role", String.class);
      for (String member : membersResult) {
        members.add(new Member(member, roleName));
      }
    }

    return members;
  }

  static void executeRemoveMembers(SqlDatabase db, String schemaName, List<Member> members) {
    try {
      SqlSchema schema = db.getSchema(schemaName);

      List<String> usernames = new ArrayList<>();
      for (Member m : members) usernames.add(m.getUser());

      for (Member m : schema.getMembers()) {
        if (usernames.contains(m.getUser())) {
          // Remove user from group and revoke PostgreSQL role (replaces trigger-based approach)
          SqlPermissionExecutor.removeUserFromGroup(db.getJooq(), m.getRole(), m.getUser());
        }
      }
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Remove of member failed", dae);
    }
  }

  // TODO: get this form group_metadata?
  static List<String> executeGetRoles(DSLContext jooq, String schemaName) {
    List<String> result = new ArrayList<>();
    for (Record r :
        jooq.fetch(
            "select rolname from pg_catalog.pg_roles where rolname LIKE {0}",
            getRolePrefix(schemaName) + "%")) {
      result.add(r.getValue("rolname", String.class).substring(getRolePrefix(schemaName).length()));
    }
    return result;
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

      // update metadata before so policy triggers fire
      MetadataUtils.deleteSchema(db.getJooq(), schemaName);

      // remove tables individually to trigger foreign key error if appropriate
      List<Table> tables = db.getSchema(schemaName).getTablesSorted();
      Collections.reverse(tables);
      tables.forEach(table -> executeDropTable(db.getJooq(), table.getMetadata()));

      // drop schema
      db.getJooq().dropSchema(name(schemaName)).execute();

      for (String role : executeGetRoles(db.getJooq(), schemaName)) {
        db.getJooq().execute("DROP ROLE IF EXISTS {0}", name(getRolePrefix(schemaName) + role));
      }
    } catch (Exception e) {
      throw new SqlMolgenisException("Drop schema failed", e);
    }
  }
}

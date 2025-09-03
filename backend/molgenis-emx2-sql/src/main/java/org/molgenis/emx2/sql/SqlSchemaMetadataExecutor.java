package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.MetadataUtils.*;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeDropTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.*;

class SqlSchemaMetadataExecutor {

  private SqlSchemaMetadataExecutor() {
    // hide
  }

  static void executeCreateSchema(SqlDatabase db, SchemaMetadata schema) {
    db.getJooqWithExtendedTimeout().createSchema(schema.getName()).execute();

    MetadataUtils.saveSchemaMetadata(db.getJooqWithExtendedTimeout(), schema);
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

    // Trigger wil grant permissions
    String groupName = schema.getName() + "/" + member.getRole();
    jooq.update(GROUP_METADATA)
        .set(
            USERS,
            DSL.when(
                    DSL.not(DSL.condition("? = any(" + USERS + ")", member.getUser())),
                    DSL.arrayAppend(USERS, member.getUser()))
                .otherwise(USERS))
        .where(GROUP_NAME.eq(groupName))
        .execute();
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

  static List<Member> executeGetMembers(DSLContext jooq, SchemaMetadata schema) {
    List<Member> members = new ArrayList<>();

    // retrieve all role members TODO: get this from group_metadata?
    String roleFilter = getRolePrefix(schema.getName());
    String userFilter = Constants.MG_USER_PREFIX;
    List<Record> result =
        jooq.fetch(
            "select distinct m.rolname as member, r.rolname as role"
                + " from pg_catalog.pg_auth_members am "
                + " join pg_catalog.pg_roles m on (m.oid = am.member)"
                + "join pg_catalog.pg_roles r on (r.oid = am.roleid)"
                + "where r.rolname LIKE {0} and m.rolname LIKE {1}",
            roleFilter + "%", userFilter + "%");
    for (Record r : result) {
      String memberName = r.getValue("member", String.class).substring(userFilter.length());
      String roleName = r.getValue("role", String.class).substring(roleFilter.length());
      members.add(new Member(memberName, roleName));
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
          String groupName = schema.getName() + "/" + m.getRole();
          db.getJooq()
              .update(GROUP_METADATA)
              .set(USERS, DSL.arrayRemove(USERS, m.getUser()))
              .where(GROUP_NAME.eq(groupName))
              .execute();
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

      // remove tables individually to trigger foreign key error if appropriate
      List<Table> tables = db.getSchema(schemaName).getTablesSorted();
      Collections.reverse(tables);
      tables.forEach(table -> executeDropTable(db.getJooq(), table.getMetadata()));

      // drop schema
      db.getJooq().dropSchema(name(schemaName)).execute();

      for (String role : executeGetRoles(db.getJooq(), schemaName)) {
        db.getJooq().execute("DROP ROLE IF EXISTS {0}", name(getRolePrefix(schemaName) + role));
      }
      MetadataUtils.deleteSchema(db.getJooq(), schemaName);
    } catch (Exception e) {
      throw new SqlMolgenisException("Drop schema failed", e);
    }
  }
}

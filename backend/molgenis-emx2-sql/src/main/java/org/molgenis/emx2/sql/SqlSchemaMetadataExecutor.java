package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeDropTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DDLQuery;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;

class SqlSchemaMetadataExecutor {

  private SqlSchemaMetadataExecutor() {
    // hide
  }

  static void executeCreateSchema(SqlDatabase db, SchemaMetadata schema) {
    DDLQuery step = db.getJooq().createSchema(schema.getName());
    step.execute();

    String schemaName = schema.getName();
    String aggregator = getRolePrefix(schemaName) + AGGREGATOR;
    String viewer = getRolePrefix(schemaName) + VIEWER;
    String editor = getRolePrefix(schemaName) + EDITOR;
    String manager = getRolePrefix(schemaName) + MANAGER;
    String owner = getRolePrefix(schemaName) + Privileges.OWNER;

    db.addRole(aggregator);
    db.addRole(viewer);
    db.addRole(editor);
    db.addRole(manager);
    db.addRole(owner);

    // make viewer also aggregator
    db.getJooq().execute("GRANT {0} TO {1}", name(aggregator), name(viewer));

    // make editor also viewer
    db.getJooq().execute("GRANT {0} TO {1}", name(viewer), name(editor));

    // make manager also editor, viewer and aggregator
    db.getJooq()
        .execute(
            "GRANT {0},{1},{2} TO {3} WITH ADMIN OPTION",
            name(aggregator), name(viewer), name(editor), name(manager));

    // make owner also editor, manager, member
    db.getJooq()
        .execute(
            "GRANT {0},{1},{2},{3} TO {4} WITH ADMIN OPTION",
            name(aggregator), name(viewer), name(editor), name(manager), name(owner));

    String currentUser = db.getJooq().fetchOne("SELECT current_user").get(0, String.class);
    String sessionUser = db.getJooq().fetchOne("SELECT session_user").get(0, String.class);

    // make current user the owner
    if (!sessionUser.equals(currentUser)) {
      db.getJooq().execute("GRANT {0} TO {1}", name(manager), name(currentUser));
    }

    // make admin owner
    db.getJooq().execute("GRANT {0} TO {1}", name(manager), name(sessionUser));

    // grant the permissions
    db.getJooq()
        .execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schema.getName()), name(aggregator));
    db.getJooq().execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schema.getName()), name(manager));

    MetadataUtils.saveSchemaMetadata(db.getJooq(), schema);
  }

  static void executeAddMembers(DSLContext jooq, Schema schema, Member member) {
    List<String> currentRoles = schema.getRoles();
    List<Member> currentMembers = schema.getMembers();

    if (!currentRoles.contains(member.getRole())) {
      throw new MolgenisException(
          "Add member(s) failed: Role '"
              + member.getRole()
              + " doesn't exist in schema '"
              + schema.getMetadata().getName()
              + "'. Existing roles are: "
              + currentRoles);
    }
    String username = Constants.MG_USER_PREFIX + member.getUser();
    String roleprefix = getRolePrefix(schema.getMetadata().getName());
    String rolename = roleprefix + member.getRole();

    // execute updates database
    updateMembershipForUser(
        jooq,
        schema.getDatabase(),
        schema.getMetadata(),
        currentMembers,
        member,
        username,
        rolename);
  }

  private static void updateMembershipForUser(
      DSLContext jooq,
      Database db,
      SchemaMetadata schema,
      List<Member> currentMembers,
      Member m,
      String username,
      String rolename) {
    try {
      // add user if not exists
      if (!db.hasUser(m.getUser())) {
        db.addUser(m.getUser());
      }

      // revoke other roles if user has them
      for (Member old : currentMembers) {
        if (old.getUser().equals(m.getUser())) {
          jooq.execute(
              "REVOKE {0} FROM {1}",
              name(getRolePrefix(schema.getName()) + old.getRole()), name(username));
        }
      }
      jooq.execute("GRANT {0} TO {1}", name(rolename), name(username));
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Add member failed", dae);
    }
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

    // retrieve all role members
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

      String userprefix = Constants.MG_USER_PREFIX;
      String roleprefix = getRolePrefix(schema.getMetadata().getName());

      for (Member m : schema.getMembers()) {
        if (usernames.contains(m.getUser())) {

          db.getJooq()
              .execute(
                  "REVOKE {0} FROM {1}",
                  name(roleprefix + m.getRole()), name(userprefix + m.getUser()));
        }
      }
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Remove of member failed", dae);
    }
  }

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

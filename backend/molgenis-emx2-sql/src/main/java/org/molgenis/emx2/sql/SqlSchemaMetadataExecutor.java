package org.molgenis.emx2.sql;

import org.jooq.CreateSchemaFinalStep;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;

import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.name;

class SqlSchemaMetadataExecutor {
  private SqlSchemaMetadataExecutor() {
    // hide
  }

  static void executeCreateSchema(SqlDatabase db, SchemaMetadata schema) {
    try (CreateSchemaFinalStep step = db.getJooq().createSchema(schema.getName())) {
      step.execute();

      String member = getRolePrefix(schema) + DefaultRoles.VIEWER;
      String editor = getRolePrefix(schema) + DefaultRoles.EDITOR;
      String manager = getRolePrefix(schema) + DefaultRoles.MANAGER;
      String owner = getRolePrefix(schema) + DefaultRoles.OWNER;

      db.addRole(member);
      db.addRole(editor);
      db.addRole(manager);
      db.addRole(owner);

      // make editor also member
      db.getJooq().execute("GRANT {0} TO {1}", name(member), name(editor));
      // make manager also editor and member
      db.getJooq().execute("GRANT {0},{1} TO {2}", name(member), name(editor), name(manager));
      // make owner also editor, manager, member
      db.getJooq()
          .execute(
              "GRANT {0},{1},{2} TO {3} WITH ADMIN OPTION",
              name(member), name(editor), name(manager), name(owner));

      // make current user the owner
      String currentUser = db.getJooq().fetchOne("SELECT current_user").get(0, String.class);
      db.getJooq().execute("GRANT {0} TO {1}", name(manager), name(currentUser));

      // grant the permissions
      db.getJooq()
          .execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schema.getName()), name(member));
      db.getJooq().execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schema.getName()), name(manager));
    } catch (DataAccessException e) {
      throw new SqlMolgenisException("Schema create failed", e);
    }
    MetadataUtils.saveSchemaMetadata(db.getJooq(), schema);
  }

  static void executeAddMembers(DSLContext jooq, Schema schema, List<Member> members) {
    List<String> currentRoles = schema.getRoles();
    List<Member> currentMembers = schema.getMembers();

    for (Member m : members) {
      if (!currentRoles.contains(m.getRole()))
        throw new MolgenisException(
            "Add member(s) failed",
            "Role '"
                + m.getRole()
                + " doesn't exist in schema '"
                + schema.getMetadata().getName()
                + "'. Existing roles are: "
                + currentRoles);

      String username = Constants.MG_USER_PREFIX + m.getUser();
      String roleprefix = getRolePrefix(schema.getMetadata());
      String rolename = roleprefix + m.getRole();

      // execute updates database
      updateMembershipForUser(
          jooq, schema.getDatabase(), schema.getMetadata(), currentMembers, m, username, rolename);
    }
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

      // give god powers if 'owner'
      if (DefaultRoles.OWNER.toString().equals(m.getRole())) {
        jooq.execute("ALTER ROLE {0} CREATEROLE", name(username));
      }

      // revoke other roles if user has them
      for (Member old : currentMembers) {
        if (old.getUser().equals(m.getUser())) {
          jooq.execute(
              "REVOKE {0} FROM {1}", name(getRolePrefix(schema) + old.getRole()), name(username));
        }
      }

      // grant the new role
      jooq.execute("GRANT {0} TO {1}", name(rolename), name(username));
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Add member failed", dae);
    }
  }

  static String getRolePrefix(SchemaMetadata schema) {
    return Constants.MG_ROLE_PREFIX + schema.getName().toUpperCase() + "/";
  }

  static List<Member> executeGetMembers(DSLContext jooq, SchemaMetadata schema) {
    List<Member> members = new ArrayList<>();

    // retrieve all role members
    String roleFilter = getRolePrefix(schema);
    String userFilter = Constants.MG_USER_PREFIX;
    List<Record> result =
        jooq.fetch(
            "select m.rolname as member, r.rolname as role"
                + " from pg_catalog.pg_auth_members am "
                + " join pg_catalog.pg_roles m on (m.oid = am.member)"
                + "join pg_catalog.pg_roles r on (r.oid = am.roleid)"
                + "where r.rolname ILIKE {0} and m.rolname ILIKE {1}",
            roleFilter + "%", userFilter + "%");
    for (Record r : result) {
      String memberName = r.getValue("member", String.class).substring(userFilter.length());
      String roleName = r.getValue("role", String.class).substring(roleFilter.length());
      members.add(new Member(memberName, roleName));
    }

    return members;
  }

  static void executeRemoveMembers(DSLContext jooq, SqlSchema schema, List<Member> members) {
    try {
      List<String> usernames = new ArrayList<>();
      for (Member m : members) usernames.add(m.getUser());

      String userprefix = Constants.MG_USER_PREFIX;
      String roleprefix = getRolePrefix(schema.getMetadata());

      for (Member m : schema.getMembers()) {
        if (usernames.contains(m.getUser())) {

          jooq.execute(
              "REVOKE {0} FROM {1}",
              name(roleprefix + m.getRole()), name(userprefix + m.getUser()));
        }
      }
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Remove of member failed", dae);
    }
  }

  static List<String> executeGetRoles(DSLContext jooq, SqlSchema schema) {
    List<String> result = new ArrayList<>();
    for (Record r :
        jooq.fetch(
            "select rolname from pg_catalog.pg_roles where rolname LIKE {0}",
            getRolePrefix(schema.getMetadata()) + "%")) {
      result.add(
          r.getValue("rolname", String.class)
              .substring(getRolePrefix(schema.getMetadata()).length()));
    }
    return result;
  }

  static void executeDropSchema(DSLContext jooq, SchemaMetadata schema) {
    try {
      jooq.dropSchema(name(schema.getName())).cascade().execute();
      MetadataUtils.deleteSchema(jooq, schema);
    } catch (MolgenisException me) {
      throw new MolgenisException("Drop schema failed", me.getMessage());
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Drop schema failed", dae);
    }
  }
}

package org.molgenis.emx2.sql;

import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.*;

import static org.jooq.impl.DSL.*;

public class SqlSchema implements Schema {
  private SqlDatabase db;
  private SqlSchemaMetadata metadata;

  public SqlSchema(SqlDatabase db, SqlSchemaMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public SqlTable getTable(String name) {
    SqlTableMetadata tableMetadata = getMetadata().getTableMetadata(name);
    if (tableMetadata == null) return null;
    if (tableMetadata.exists()) return new SqlTable(db, tableMetadata);
    else return null;
  }

  @Override
  public void dropTable(String name) {
    getMetadata().dropTable(name);
  }

  @Override
  public List<Member> getMembers() {
    List<Member> members = new ArrayList<>();

    // retrieve all role members
    String roleFilter = getRolePrefix();
    String userFilter = Constants.MG_USER_PREFIX;
    List<Record> result =
        db.getJooq()
            .fetch(
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

  @Override
  public void addMembers(List<Member> members) {
    transaction(
        database -> {
          List<String> currentRoles = getRoles();
          List<Member> currentMembers = getMembers();

          for (Member m : members) {
            if (!currentRoles.contains(m.getRole()))
              throw new MolgenisException(
                  "add_members_failed",
                  "Add member(s) failed",
                  "Role '"
                      + m.getRole()
                      + " doesn't exist in schema '"
                      + getMetadata().getName()
                      + "'. Existing roles are: "
                      + currentRoles);

            String username = Constants.MG_USER_PREFIX + m.getUser();
            String roleprefix = getRolePrefix();
            String rolename = roleprefix + m.getRole();

            // execute updates database
            updateMembershipForUser(currentMembers, m, username, rolename);
          }
        });
  }

  private void updateMembershipForUser(
      List<Member> currentMembers, Member m, String username, String rolename) {
    try {
      // add user if not exists
      db.addUser(m.getUser());

      // give god powers if 'owner'
      if (DefaultRoles.OWNER.toString().equals(m.getRole())) {
        db.getJooq().execute("ALTER ROLE {0} CREATEROLE", name(username));
      }

      // revoke other roles if user has them
      for (Member old : currentMembers) {
        if (old.getUser().equals(m.getUser())) {
          db.getJooq()
              .execute(
                  "REVOKE {0} FROM {1}", name(getRolePrefix() + old.getRole()), name(username));
        }
      }

      // grant the new role
      db.getJooq().execute("GRANT {0} TO {1}", name(rolename), name(username));
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("add_members_failed", "Add member failed", dae);
    }
  }

  @Override
  public void addMembers(Member... members) {
    this.addMembers(Arrays.asList(members));
  }

  @Override
  public void addMember(String user, String role) {
    this.addMembers(new Member(user, role));
  }

  @Override
  public void removeMembers(List<Member> members) {
    List<String> usernames = new ArrayList<>();
    for (Member m : members) usernames.add(m.getUser());

    String userprefix = Constants.MG_USER_PREFIX;
    String roleprefix = getRolePrefix();

    transaction(
        database -> {
          for (Member m : getMembers()) {
            if (usernames.contains(m.getUser())) {
              try {
                db.getJooq()
                    .execute(
                        "REVOKE {0} FROM {1}",
                        name(roleprefix + m.getRole()), name(userprefix + m.getUser()));
              } catch (DataAccessException dae) {
                throw new SqlMolgenisException(
                    "remove_member_failed", "Remove of member failed", dae);
              }
            }
          }
        });
  }

  @Override
  public void removeMember(String user) {
    removeMembers(new Member(user, null));
  }

  @Override
  public void removeMembers(Member... members) {
    removeMembers(Arrays.asList(members));
  }

  @Override
  public List<String> getRoles() {
    List<String> result = new ArrayList<>();
    for (Record r :
        db.getJooq()
            .fetch(
                "select rolname from pg_catalog.pg_roles where rolname LIKE {0}",
                getRolePrefix() + "%")) {
      result.add(r.getValue("rolname", String.class).substring(getRolePrefix().length()));
    }
    return result;
  }

  private String getRolePrefix() {
    return getMetadata().getRolePrefix();
  }

  @Override
  public String getRoleForUser(String user) {
    user = user.trim();
    for (Member m : getMembers()) {
      if (m.getUser().equals(user)) return m.getRole();
    }
    return null;
  }

  @Override
  public Table createTableIfNotExists(String name) {
    Table exists = getTable(name);
    if (exists != null) {
      return exists;
    } else {
      getMetadata().createTable(name);
      return getTable(name);
    }
  }

  @Override
  public Table createTableIfNotExists(TableMetadata metadata) {
    transaction(
        database -> {
          TableMetadata table = this.createTableIfNotExists(metadata.getTableName()).getMetadata();
          for (Column c : metadata.getColumns()) {
            table.addColumn(c);
          }
          if (metadata.getPrimaryKey().length > 0) table.setPrimaryKey(metadata.getPrimaryKey());
          for (String[] unique : metadata.getUniques()) {
            table.addUnique(unique);
          }
        });
    return getTable(metadata.getTableName());
  }

  @Override
  public SqlSchemaMetadata getMetadata() {
    return metadata;
  }

  @Override
  public Collection<String> getTableNames() {
    return getMetadata().getTableNames();
  }

  @Override
  public Query query(String tableName) {
    return getTable(tableName).query();
  }

  @Override
  public void transaction(Transaction transaction) {
    db.transaction(transaction);
  }

  @Override
  public void merge(SchemaMetadata from) {
    transaction(
        database -> {
          List<TableMetadata> tableList = new ArrayList<>();
          for (String tableName : from.getTableNames()) {
            tableList.add(from.getTableMetadata(tableName));
          }

          // uses TableMetadata.compareTo.  todo circular dependencies
          sort(tableList);

          for (TableMetadata table : tableList) {
            this.createTableIfNotExists(table);
          }
        });
  }

  private void sort(List<TableMetadata> tableList) {
    ArrayList<TableMetadata> result = new ArrayList<>();
    ArrayList<TableMetadata> todo = new ArrayList<>(tableList);

    while (!todo.isEmpty()) {
      for (int i = 0; i < todo.size(); i++) {
        TableMetadata current = todo.get(i);
        boolean depends = false;
        for (int j = 0; j < todo.size(); j++) {
          if (dependsOn(current, todo.get(j), new ArrayList<>())) {
            depends = true;
            break;
          }
        }
        if (!depends) {
          result.add(todo.get(i));
          todo.remove(i);
          break;
        }
      }
    }
    tableList.clear();
    tableList.addAll(result);
  }

  /** check for reference */
  private boolean dependsOn(TableMetadata from, TableMetadata to, List<String> visited) {
    // visited is prevent circular relations to take up the loop forever
    visited.add(from.getTableName());
    for (Column c : from.getColumns()) {
      if (c.getRefTableName() != null) {
        if (from.getSchema().getTableMetadata(c.getRefTableName()) == null)
          throw new MolgenisException(
              "invalid_reference",
              "invalid_reference",
              "Reference '"
                  + c.getColumnName()
                  + "' from '"
                  + from.getTableName()
                  + "' to '"
                  + c.getRefTableName()
                  + "' failed. Table '"
                  + c.getRefTableName()
                  + "' could not be found");
        if (c.getRefTableName().equals(to.getTableName())) return true;
        // recurse
        if (!visited.contains(c.getRefTableName())
            && dependsOn(from.getSchema().getTableMetadata(c.getRefTableName()), to, visited)) {
          return true;
        }
      }
    }
    if (from.getInherit() != null) {
      if (from.getInherit().equals(to.getTableName())) return true;
      if (dependsOn(from.getInheritedTable(), to, visited)) return true;
    }
    return false;
  }
}

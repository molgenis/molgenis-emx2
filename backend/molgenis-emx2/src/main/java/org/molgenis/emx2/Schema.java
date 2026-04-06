package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Schema {

  default BundleContext getBundleContext() {
    return null;
  }

  default boolean isBundleBacked() {
    return getBundleContext() != null;
  }

  default boolean isBundleLocked() {
    return false;
  }

  Database getDatabase();

  SchemaMetadata getMetadata();

  Collection<String> getTableNames();

  List<String> getInheritedRolesForActiveUser();

  boolean hasActiveUserRole(Privileges privileges);

  Table create(TableMetadata table);

  void create(TableMetadata... table);

  Table getTable(String name);

  List<Table> getTablesSorted();

  void dropTable(String name);

  Query query(String tableName);

  List<Row> retrieveSql(String sql);

  List<Row> retrieveSql(String sql, Map<String, ?> params);

  Query agg(String tableName);

  Query groupBy(String tableName);

  Query query(String field, SelectColumn... selection);

  void migrate(SchemaMetadata from);

  void tx(Transaction transaction);

  void enableProfile(String profileName);

  void disableProfile(String profileName);

  Set<String> getActiveProfiles();

  void discard(SchemaMetadata schema);

  void addMember(String user, String role);

  List<Member> getMembers();

  void removeMembers(Member... members);

  void removeMembers(List<Member> members);

  void removeMember(String user);

  List<String> getRoles();

  String getRoleForUser(String user);

  List<String> getInheritedRolesForUser(String user);

  String getRoleForActiveUser();

  String getName();

  default List<Change> getChanges(int limit) {
    return getChanges(limit, 0);
  }

  List<Change> getChanges(int limit, int offset);

  Integer getChangesCount();

  String getSettingValue(String key);

  boolean hasSetting(String emailHost);

  Table getTableById(String id);

  Table getTableByNameOrIdCaseInsensitive(String name);

  boolean hasTableWithNameOrIdCaseInsensitive(String fileName);

  void createRole(String roleName);

  void deleteRole(String roleName);

  void grant(String roleName, TablePermission permission);

  void revoke(String roleName, String tableName);

  Role getRoleInfo(String roleName);

  List<Role> getRoleInfos();

  List<TablePermission> getPermissionsForActiveUser();
}

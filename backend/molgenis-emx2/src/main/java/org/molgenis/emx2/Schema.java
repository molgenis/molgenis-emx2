package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Schema {

  Database getDatabase();

  SchemaMetadata getMetadata();

  Collection<String> getTableNames();

  List<String> getInheritedRolesForActiveUser();

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

  List<Change> getChanges(int limit);

  Integer getChangesCount();
}

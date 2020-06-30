package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;

public interface Schema {

  Database getDatabase();

  SchemaMetadata getMetadata();

  Collection<String> getTableNames();

  Table create(TableMetadata table);

  void create(TableMetadata... table);

  Table getTable(String name);

  void dropTable(String name);

  Query query(String tableName);

  void tx(Transaction transaction);

  void merge(SchemaMetadata from);

  void discard(SchemaMetadata schema);

  void addMembers(List<Member> members);

  void addMembers(Member... members);

  void addMember(String user, String role);

  List<Member> getMembers();

  void removeMembers(Member... members);

  void removeMembers(List<Member> members);

  void removeMember(String user);

  List<String> getRoles();

  String getRoleForUser(String user);
}

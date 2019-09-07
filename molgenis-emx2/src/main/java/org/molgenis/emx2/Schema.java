package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.util.Collection;
import java.util.List;

public interface Schema {

  SchemaMetadata getMetadata();

  Collection<String> getTableNames() throws MolgenisException;

  void addMembers(List<Member> members) throws MolgenisException;

  void addMembers(Member... members) throws MolgenisException;

  void addMember(String user, String role) throws MolgenisException;

  Table createTableIfNotExists(String name) throws MolgenisException;

  Table create(TableMetadata table) throws MolgenisException;

  Table getTable(String name) throws MolgenisException;

  List<Member> getMembers() throws MolgenisException;

  Query query(String tableName) throws MolgenisException;

  void transaction(Transaction transaction) throws MolgenisException;

  void transaction(String role, Transaction transaction) throws MolgenisException;

  void merge(SchemaMetadata from) throws MolgenisException;

  void removeMembers(Member... members) throws MolgenisException;

  void removeMembers(List<Member> members) throws MolgenisException;

  void removeMember(String user1) throws MolgenisException;

  List<String> getRoles();

  String getRoleForUser(String user) throws MolgenisException;
}

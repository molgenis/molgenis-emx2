package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.util.Collection;

public interface Schema {

  SchemaMetadata getMetadata();

  Collection<String> getTableNames() throws MolgenisException;

  Table createTableIfNotExists(String name) throws MolgenisException;

  Table create(TableMetadata table) throws MolgenisException;

  Table getTable(String name) throws MolgenisException;

  void grant(Permission permission, String role) throws MolgenisException;

  void revokePermission(Permission permission, String role) throws MolgenisException;

  Query query(String tableName) throws MolgenisException;

  void transaction(Transaction transaction) throws MolgenisException;

  void transaction(String role, Transaction transaction) throws MolgenisException;

  void copy(SchemaMetadata from) throws MolgenisException;
}

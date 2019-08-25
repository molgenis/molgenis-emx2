package org.molgenis.data;

import org.molgenis.MolgenisException;
import org.molgenis.Permission;
import org.molgenis.metadata.SchemaMetadata;
import org.molgenis.query.Query;

import java.util.Collection;

public interface Schema {

  SchemaMetadata getMetadata();

  Collection<String> getTableNames() throws MolgenisException;

  Table createTableIfNotExists(String name) throws MolgenisException;

  Table getTable(String name) throws MolgenisException;

  void grant(Permission permission, String role) throws MolgenisException;

  void revokePermission(Permission permission, String role) throws MolgenisException;

  Query query(String tableName) throws MolgenisException;
}

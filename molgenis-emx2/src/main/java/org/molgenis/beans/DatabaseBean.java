package org.molgenis.beans;

import org.molgenis.*;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class DatabaseBean implements Database {
  private String name;
  protected Map<String, Schema> schemas = new LinkedHashMap<>();

  /** for subclasses to addSchema internally */
  protected void addSchema(Schema schema) {
    this.schemas.put(schema.getName(), schema);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Schema createSchema(String name) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void createUser(String name) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Schema getSchema(String name) throws MolgenisException {
    return this.schemas.get(name);
  }

  @Override
  public void grantRoleToUser(String role, String user) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void transaction(Transaction transactionb) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void transaction(Transaction transaction, String role) throws MolgenisException {
    throw new UnsupportedOperationException();
  }
}

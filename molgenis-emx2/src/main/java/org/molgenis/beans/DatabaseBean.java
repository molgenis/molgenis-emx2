package org.molgenis.beans;

import org.molgenis.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseBean implements Database {
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
    Schema result = this.schemas.get(name);
    if (result == null) throw new MolgenisException("Schema '" + name + " doesn't exist");
    return result;
  }

  @Override
  public Collection<String> getSchemaNames() throws MolgenisException {
    return this.schemas.keySet();
  }

  @Override
  public void grantRole(String role, String user) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void transaction(Transaction transactionb) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void transaction(String role, Transaction transaction) throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearCache() {
    this.schemas.clear();
  }

  @Override
  public void setDeferChecks(boolean shouldDefer) {
    throw new UnsupportedOperationException();
  };
}

package org.molgenis;

import java.util.Collection;

public interface Database {

  String getName();

  void setName(String name);

  Schema createSchema(String name) throws MolgenisException;

  Schema getSchema(String name) throws MolgenisException;

  void transaction(Transaction transaction) throws MolgenisException;
}

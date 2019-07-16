package org.molgenis;

@FunctionalInterface
public interface Transaction {
  void run(Database database) throws MolgenisException;
}

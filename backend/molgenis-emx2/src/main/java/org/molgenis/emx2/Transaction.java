package org.molgenis.emx2;

@FunctionalInterface
public interface Transaction {
  void run(Database database);
}

package org.molgenis;

import org.molgenis.data.Database;

@FunctionalInterface
public interface Transaction {
  void run(Database database) throws MolgenisException;
}

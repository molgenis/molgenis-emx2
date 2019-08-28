package org.molgenis;

import org.molgenis.utils.MolgenisException;

@FunctionalInterface
public interface Transaction {
  void run(Database database) throws MolgenisException;
}

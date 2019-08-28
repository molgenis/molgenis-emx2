package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

@FunctionalInterface
public interface Transaction {
  void run(Database database) throws MolgenisException;
}

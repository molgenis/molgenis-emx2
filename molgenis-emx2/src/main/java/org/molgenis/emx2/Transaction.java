package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.io.IOException;

@FunctionalInterface
public interface Transaction {
  void run(Database database) throws MolgenisException, IOException;
}

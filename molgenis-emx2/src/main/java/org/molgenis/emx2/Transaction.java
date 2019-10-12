package org.molgenis.emx2;

import java.io.IOException;

@FunctionalInterface
public interface Transaction {
  void run(Database database) throws IOException;
}

package org.molgenis.emx2.fairmapper.load;

import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

public interface DataLoader {

  void load(InMemoryTableStore tableStore);
}

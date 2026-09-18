package org.molgenis.emx2.fairmapper.load;

import org.molgenis.emx2.io.tablestore.TableStore;

public interface DataLoader {

  void load(TableStore tableStore);
}

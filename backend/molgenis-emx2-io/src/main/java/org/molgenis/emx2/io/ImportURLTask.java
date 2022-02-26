package org.molgenis.emx2.io;

import java.net.URL;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStoreForURL;

public class ImportURLTask extends ImportSchemaTask {

  public ImportURLTask(URL url, Schema schema, boolean strict) {
    super("Import from url " + url, new TableStoreForURL(url), schema, strict);
  }
}

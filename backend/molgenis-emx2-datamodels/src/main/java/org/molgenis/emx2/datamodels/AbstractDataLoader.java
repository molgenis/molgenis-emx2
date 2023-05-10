package org.molgenis.emx2.datamodels;

import java.io.InputStreamReader;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public abstract class AbstractDataLoader {
  // using to ensure shared schemas are not created together.
  private static final Object lock = new Object();

  public void load(Schema schema, boolean includeExampleData) {
    // make sure only one install runs because of potential overlap with other installers
    // e.g. shared schema
    synchronized (lock) {
      // make sure all installation stuff is in one tx
      schema.tx(
          db -> {
            try {
              this.loadInternalImplementation(db.getSchema(schema.getName()), includeExampleData);
            } catch (Exception e) {
              throw new MolgenisException(e.getMessage());
            }
          });
    }
  }

  abstract void loadInternalImplementation(Schema schema, boolean includeDemoData);

  public static void createSchema(Schema schema, String path) {
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    DataCatalogueLoader.class.getClassLoader().getResourceAsStream(path))));
    schema.migrate(metadata);
  }
}

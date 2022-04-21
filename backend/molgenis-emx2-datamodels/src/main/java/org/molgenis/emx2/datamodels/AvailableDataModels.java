package org.molgenis.emx2.datamodels;

import java.io.IOException;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

public enum AvailableDataModels {
  DATA_CATALOGUE(new DataCatalogueLoader()),
  DATA_CATALOGUE_STAGING(new DataCatalogueStagingLoader()),
  PET_STORE(new PetStoreLoader());

  private DataModelLoader installer;

  AvailableDataModels(DataModelLoader installer) {
    this.installer = installer;
  }

  public void install(Schema schema, boolean includeExampleData) {
    schema.tx(
        db -> {
          try {
            // make sure not to use schema but db because in a transaction!
            installer.load(db.getSchema(schema.getName()), includeExampleData);
          } catch (Exception e) {
            throw new MolgenisException(e.getMessage());
          }
        });
  }

  public interface DataModelLoader {
    void load(Schema schema, boolean includeExampleData) throws IOException;
  }
}

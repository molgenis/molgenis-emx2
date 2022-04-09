package org.molgenis.emx2.datamodels;

import java.io.IOException;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

public enum AvailableLoadersEnum {
  DATA_CATALOGUE(new DataCatalogueLoader()),
  DATA_CATALOGUE_STAGING(new DataCatalogueStagingLoader()),
  PET_STORE(new PetStoreLoader());

  private DataModelLoader installer;

  AvailableLoadersEnum(DataModelLoader installer) {
    this.installer = installer;
  }

  public void install(Schema schema, boolean includeExampleData) {
    schema.tx(
        db -> {
          try {
            installer.loadMetadata(schema);
            if (includeExampleData) {
              installer.loadExampleData(schema);
            }
          } catch (Exception e) {
            throw new MolgenisException(e.getMessage());
          }
        });
  }

  public interface DataModelLoader {

    void loadMetadata(Schema schema) throws IOException;

    void loadExampleData(Schema schema);
  }
}

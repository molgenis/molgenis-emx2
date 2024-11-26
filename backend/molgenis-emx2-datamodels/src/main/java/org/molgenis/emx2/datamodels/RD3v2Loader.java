package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.profiles.SchemaFromProfile.getProfilesFromAllModels;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.emx2.Emx2;

public class RD3v2Loader extends ImportDataModelTask {

  public static final String ONTOLOGIES = "CatalogueOntologies";

  private boolean staging = false;

  public RD3v2Loader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    String location = "portal/";
    Database db = getSchema().getDatabase();
    Schema ontologySchema = db.getSchema(ONTOLOGIES);
    if (ontologySchema == null) {
      db.createSchema(ONTOLOGIES);
    }

    // create RD3v2 without profile filtering
    List<Row> rows = null;
    try {
      rows = getProfilesFromAllModels("/portal", List.of());
      getSchema().migrate(Emx2.fromRowList(rows));
    } catch (Exception e) {
      throw new MolgenisException("Create profile failed", e);
    }
    this.complete();
  }

  public ImportDataModelTask setStaging(boolean staging) {
    this.staging = staging;
    return this;
  }
}

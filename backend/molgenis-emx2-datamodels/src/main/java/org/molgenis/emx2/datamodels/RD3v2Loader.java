package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.profiles.SchemaFromProfile.getProfilesFromAllModels;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.emx2.Emx2;

public class RD3v2Loader extends ImportDataModelTask {

  public static final String ONTOLOGIES = "CatalogueOntologies";

  public RD3v2Loader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    Database db = getSchema().getDatabase();
    // create RD3v2 without profile filtering
    try {
      if (!db.getSchemaNames().contains(ONTOLOGIES)) {
        db.createSchema(ONTOLOGIES);
      }
      List<Row> rows = getProfilesFromAllModels("/portal", List.of());
      getSchema().migrate(Emx2.fromRowList(rows));
      this.complete();
    } catch (Exception e) {
      this.setError(e.getMessage());
      throw new MolgenisException("Create profile failed", e);
    }
  }
}

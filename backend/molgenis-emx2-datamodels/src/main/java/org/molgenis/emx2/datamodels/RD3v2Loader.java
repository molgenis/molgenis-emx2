package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.datamodels.profiles.SchemaFromProfile.getProfilesFromAllModels;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportDataModelTask;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.tasks.Task;

public class RD3v2Loader extends ImportDataModelTask {

  public static final String ONTOLOGIES = "CatalogueOntologies";
  public static final String CATALOGUE = "catalogue";

  public RD3v2Loader(Schema schema, Boolean includeDemoData) {
    super(schema, includeDemoData);
  }

  @Override
  public void run() {
    this.start();
    Database db = getSchema().getDatabase();
    // create RD3v2 without profile filtering
    try {
      if (!db.getSchemaNames().contains(CATALOGUE)) {
        Schema catalogueSchema = db.createSchema(CATALOGUE);
        Task task = DataModels.Profile.DATA_CATALOGUE.getImportTask(catalogueSchema, false);
        task.setDescription("Creating catalogue first, need to rethink this");
        this.addSubTask(task);
        task.run();
      }
      List<Row> rows = getProfilesFromAllModels("/portal", List.of());
      getSchema().migrate(Emx2.fromRowList(rows));
      MolgenisIO.fromClasspathDirectory("/_ontologies", getSchema(), false);
      MolgenisIO.fromClasspathDirectory("/_settings/portal", getSchema(), false);

      if (isIncludeDemoData()) {
        MolgenisIO.fromClasspathDirectory("/_demodata/applications/portal", getSchema(), false);
      }

      this.complete();
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw new MolgenisException("Create profile failed", e);
    }
  }
}

package org.molgenis.emx2.io;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.tasks.Task;

import java.io.InputStreamReader;

public class ImportDataModelTask extends Task {

  @JsonIgnore private final Schema schema;
  private final boolean includeDemoData;

  public ImportDataModelTask(SchemaLoaderSettings schemaLoaderSettings) {
    Database database = schemaLoaderSettings.database();
    String schemaName = schemaLoaderSettings.schemaName();
    String description = schemaLoaderSettings.description();
    this.schema = database.createSchema(schemaName, description);
    this.includeDemoData = schemaLoaderSettings.includeDemoData();
  }

  public Schema getSchema() {
    return schema;
  }

  public boolean isIncludeDemoData() {
    return includeDemoData;
  }

  public void createSchema(String path) {
    createSchema(this.schema, path);
  }

  public static void createSchema(Schema schema, String path) {
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    ImportDataModelTask.class.getClassLoader().getResourceAsStream(path))));
    schema.migrate(metadata);
  }
}

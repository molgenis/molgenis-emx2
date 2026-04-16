package org.molgenis.emx2.harvester.util;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class HarvestingTestSchema {

  public static SchemaMetadata create() {
    Database database = TestDatabaseFactory.getTestDatabase();
    String schemaName = "harvesting";
    database.dropSchemaIfExists(schemaName);
    DataModels.Profile.DATA_CATALOGUE
        .getImportTask(database, schemaName, "DCAT harvesting test", true)
        .run();

    SchemaMetadata schema = database.getSchema(schemaName).getMetadata();
    TableMetadata resourcesTable = schema.getTableMetadata("Resources");

    // Add missing semantics for key column
    Column column = resourcesTable.getColumn("id");
    column.setSemantics("dcterms:title", "dcterms:alternative");

    return schema;
  }
}

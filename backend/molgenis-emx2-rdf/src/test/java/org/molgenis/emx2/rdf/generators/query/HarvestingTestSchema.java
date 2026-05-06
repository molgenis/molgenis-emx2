package org.molgenis.emx2.rdf.generators.query;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class HarvestingTestSchema {

  public static void create() {
    Database database = TestDatabaseFactory.getTestDatabase();
    String schemaName = "harvesting";
    database.dropSchemaIfExists(schemaName);
    DataModels.Profile.DATA_CATALOGUE
        .getImportTask(database, schemaName, "DCAT harvesting test", true)
        .run();

    SchemaMetadata schema = database.getSchema(schemaName).getMetadata();
    TableMetadata resourcesTable = schema.getTableMetadata("Resources");

    // Add missing semantics for key column
    Column id = resourcesTable.getColumn("id");
    id.setSemantics("dcterms:title", "dcterms:alternative");
    resourcesTable.alterColumn(id);

    // Can be modified
    Column issued = resourcesTable.getColumn("issued");
    issued.setSemantics("dcterms:issued", "fdp-o:metadataIssued");
    resourcesTable.alterColumn(issued);

    Column modified = resourcesTable.getColumn("modified");
    modified.setSemantics("dcterms:modified", "fdp-o:metadataModified");
    resourcesTable.alterColumn(modified);

    // Currently missing
    TableMetadata agents = schema.getTableMetadata("Agents");
    Column agentId = agents.getColumn("id");
    agentId.setSemantics("dcterms:id");
    agents.alterColumn(agentId);
    Column agentResource = agents.getColumn("resource");
    agentResource.setSemantics("dcterms:resource");
    agents.alterColumn(agentResource);

    TableMetadata contacts = schema.getTableMetadata("Contacts");
    Column contactResource = contacts.getColumn("resource");
    contactResource.setSemantics("dcterms:resource");
    contacts.alterColumn(contactResource);
  }
}

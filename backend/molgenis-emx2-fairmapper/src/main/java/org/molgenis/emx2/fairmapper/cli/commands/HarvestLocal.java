package org.molgenis.emx2.fairmapper.cli.commands;

import java.net.URI;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.schemas.DatabaseSchemaFetcher;
import org.molgenis.emx2.fairmapper.schemas.SchemaFetcher;
import org.molgenis.emx2.sql.SqlDatabase;
import picocli.CommandLine;

@CommandLine.Command(
    name = "local",
    description = "Harvest into a locally running database",
    mixinStandardHelpOptions = true)
public class HarvestLocal extends AbstractHarvestCommand {

  private Database database;

  @Override
  protected SchemaFetcher schemaFetcher() {
    return new DatabaseSchemaFetcher(database());
  }

  @Override
  protected HarvestingPipelineConfig.Builder configBuilder(URI rdfUri, String[] tables) {
    return HarvestingPipelineConfig.Builder.localConfig(database(), rdfUri, schemaName, tables);
  }

  private Database database() {
    if (database == null) {
      logger.info("Accessing database");
      SqlDatabase sqlDatabase = new SqlDatabase(false);
      sqlDatabase.becomeAdmin();
      database = sqlDatabase;
    }
    return database;
  }
}

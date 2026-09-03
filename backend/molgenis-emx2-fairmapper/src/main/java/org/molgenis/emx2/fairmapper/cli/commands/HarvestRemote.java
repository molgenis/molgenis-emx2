package org.molgenis.emx2.fairmapper.cli.commands;

import java.net.URI;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.schemas.GraphqlSchemaFetcher;
import org.molgenis.emx2.fairmapper.schemas.SchemaFetcher;
import org.molgenis.emx2.graphql.GraphqlClient;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remote",
    description = "Harvest into a remote emx2 instance",
    mixinStandardHelpOptions = true)
public class HarvestRemote extends AbstractHarvestCommand {

  @CommandLine.Option(
      names = {"--endpoint"},
      required = true,
      description = "Base URL of the remote emx2 instance")
  protected String endpoint;

  @CommandLine.Option(
      names = {"--token"},
      required = true,
      description = "Authentication token for the remote emx2 instance")
  protected String token;

  @Override
  protected SchemaFetcher schemaFetcher() {
    return new GraphqlSchemaFetcher(new GraphqlClient(endpoint, token));
  }

  @Override
  protected HarvestingPipelineConfig.Builder configBuilder(URI rdfUri, String[] tables) {
    return HarvestingPipelineConfig.Builder.remoteConfig(
        endpoint, token, rdfUri, schemaName, tables);
  }
}

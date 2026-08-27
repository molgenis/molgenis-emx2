package org.molgenis.emx2.fairmapper.cli.commands;

import java.net.URI;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
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
  protected HarvestingPipelineConfig.Builder buildConfigBuilder(URI rdfUri, String[] tables) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}

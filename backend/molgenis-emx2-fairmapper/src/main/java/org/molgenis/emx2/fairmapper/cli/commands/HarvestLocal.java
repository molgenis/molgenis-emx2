package org.molgenis.emx2.fairmapper.cli.commands;

import java.net.URI;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import picocli.CommandLine;

@CommandLine.Command(
    name = "local",
    description = "Harvest into a locally running database",
    mixinStandardHelpOptions = true)
public class HarvestLocal extends AbstractHarvestCommand {

  @Override
  protected HarvestingPipelineConfig.Builder buildConfigBuilder(URI rdfUri, String[] tables) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}

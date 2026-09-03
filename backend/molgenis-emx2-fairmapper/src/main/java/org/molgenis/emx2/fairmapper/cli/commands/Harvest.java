package org.molgenis.emx2.fairmapper.cli.commands;

import picocli.CommandLine;

@CommandLine.Command(
    name = "harvest",
    description = "Harvest a given endpoint",
    mixinStandardHelpOptions = true,
    subcommands = {HarvestLocal.class, HarvestRemote.class})
public class Harvest implements Runnable {

  @Override
  @SuppressWarnings("java:S106")
  public void run() {
    CommandLine.usage(this, System.out);
  }
}

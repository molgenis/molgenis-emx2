package org.molgenis.emx2.fairmapper.cli;

import org.molgenis.emx2.fairmapper.cli.commands.GenerateQuery;
import picocli.CommandLine;

@CommandLine.Command(
    name = "fairmapper",
    mixinStandardHelpOptions = true,
    version = "0.1",
    subcommands = {GenerateQuery.class},
    description = "Tool for DCAT harvesting")
public class FairMapper implements Runnable {

  public static void main(String[] args) {
    System.exit(execute(args));
  }

  public static int execute(String... args) {
    return new CommandLine(new FairMapper()).setColorScheme(createColorScheme()).execute(args);
  }

  private static CommandLine.Help.ColorScheme createColorScheme() {
    return new CommandLine.Help.ColorScheme.Builder()
        .commands(CommandLine.Help.Ansi.Style.bold, CommandLine.Help.Ansi.Style.fg_cyan)
        .options(CommandLine.Help.Ansi.Style.fg_yellow)
        .parameters(CommandLine.Help.Ansi.Style.fg_yellow)
        .optionParams(CommandLine.Help.Ansi.Style.italic)
        .build();
  }

  @Override
  @SuppressWarnings("java:S106")
  public void run() {
    CommandLine.usage(this, System.out);
  }
}

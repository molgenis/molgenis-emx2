package org.molgenis.emx2.fairmapper.cli;

import java.util.Random;
import org.molgenis.emx2.fairmapper.cli.commands.GenerateQuery;
import org.molgenis.emx2.fairmapper.cli.commands.Harvest;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;
import picocli.CommandLine;

@CommandLine.Command(
    name = "fairmapper",
    mixinStandardHelpOptions = true,
    version = "0.1",
    subcommands = {GenerateQuery.class, Harvest.class},
    description = "Tool for DCAT harvesting")
public class FairMapper implements Runnable {

  public static final Random RANDOM = new Random();

  public static void main(String[] args) {
    System.exit(execute(args));
  }

  public static int execute(String... args) {
    if (!SnowflakeIdGenerator.hasInstance()) {
      SnowflakeIdGenerator.init(String.valueOf(RANDOM.nextLong(SnowflakeIdGenerator.MAX_ID)));
    }
    
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

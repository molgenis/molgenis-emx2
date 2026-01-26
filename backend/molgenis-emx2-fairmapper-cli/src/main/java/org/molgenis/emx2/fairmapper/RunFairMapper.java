package org.molgenis.emx2.fairmapper;

import java.nio.file.Path;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Command(
    name = "fairmapper",
    description = "Create API adapters without Java code",
    mixinStandardHelpOptions = true,
    versionProvider = RunFairMapper.VersionProvider.class,
    subcommands = {
      org.molgenis.emx2.fairmapper.commands.ValidateCommand.class,
      org.molgenis.emx2.fairmapper.commands.TestCommand.class,
      org.molgenis.emx2.fairmapper.commands.DryRunCommand.class,
      org.molgenis.emx2.fairmapper.commands.E2eCommand.class,
      org.molgenis.emx2.fairmapper.commands.FetchRdfCommand.class,
      org.molgenis.emx2.fairmapper.commands.RunCommand.class
    })
public class RunFairMapper implements Runnable {

  private static final String CONFIG_FILE = "fairmapper.yaml";

  static class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
      return new String[] {"MOLGENIS FAIRmapper " + Version.getVersion()};
    }
  }

  public static void main(String[] args) {
    System.exit(execute(args));
  }

  public static int execute(String... args) {
    return new CommandLine(new RunFairMapper()).setColorScheme(createColorScheme()).execute(args);
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
  public void run() {
    CommandLine.usage(this, System.out);
  }

  public static Path resolveConfigPath(Path bundlePath) {
    return bundlePath.resolve(CONFIG_FILE);
  }

  public static String color(String text) {
    return CommandLine.Help.Ansi.AUTO.string(text);
  }
}

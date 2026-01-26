package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.molgenis.emx2.Version;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.Step;
import org.molgenis.emx2.fairmapper.model.TestCase;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Command(
    name = "fairmapper",
    description = "Create API adapters without Java code",
    mixinStandardHelpOptions = true,
    versionProvider = RunFairMapper.VersionProvider.class,
    subcommands = {
      RunFairMapper.ValidateCommand.class,
      RunFairMapper.TestCommand.class,
      RunFairMapper.DryRunCommand.class,
      RunFairMapper.E2eCommand.class
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

  static Path resolveConfigPath(Path bundlePath) {
    return bundlePath.resolve(CONFIG_FILE);
  }

  @Command(
      name = "validate",
      description = "Check FAIRmapper bundle structure and fairmapper.yaml file existence",
      mixinStandardHelpOptions = true)
  static class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the bundle directory")
    private Path bundlePath;

    @Override
    public Integer call() {
      bundlePath = bundlePath.toAbsolutePath();
      Path configPath = resolveConfigPath(bundlePath);

      System.out.println();
      System.out.println(color("@|bold,cyan Validating bundle:|@ " + bundlePath));
      System.out.println();

      try {
        BundleLoader loader = new BundleLoader();
        MappingBundle bundle = loader.load(configPath);

        System.out.println(color("  @|bold Name:|@     " + bundle.name()));
        System.out.println(color("  @|bold Version:|@  " + bundle.version()));
        System.out.println(color("  @|bold Endpoints:|@ " + bundle.endpoints().size()));
        System.out.println();

        for (Endpoint endpoint : bundle.endpoints()) {
          System.out.println(
              color(
                  "  @|yellow "
                      + endpoint.path()
                      + "|@ ["
                      + String.join(", ", endpoint.methods())
                      + "]"));
          System.out.println("    Steps: " + endpoint.steps().size());

          int testCount = countStepTests(endpoint);
          if (testCount > 0) {
            System.out.println("    Unit tests: " + testCount);
          }

          if (endpoint.e2e() != null && endpoint.e2e().tests() != null) {
            System.out.println("    E2e tests: " + endpoint.e2e().tests().size());
          }
        }

        System.out.println();
        System.out.println(color("@|bold,green ✓ Bundle valid|@"));
        System.out.println();
        return 0;

      } catch (Exception e) {
        System.err.println();
        System.err.println(color("@|bold,red ✗ Validation failed:|@ " + e.getMessage()));
        System.err.println();
        return 1;
      }
    }

    private int countStepTests(Endpoint endpoint) {
      int count = 0;
      for (Step step : endpoint.steps()) {
        if (step.tests() != null) {
          count += step.tests().size();
        }
      }
      return count;
    }
  }

  @Command(
      name = "test",
      description = "Run step-level unit tests (transforms)",
      mixinStandardHelpOptions = true)
  static class TestCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the bundle directory")
    private Path bundlePath;

    @Option(
        names = {"-v", "--verbose"},
        description = "Show detailed output for each test")
    private boolean verbose;

    @Override
    public Integer call() {
      bundlePath = bundlePath.toAbsolutePath();
      Path configPath = resolveConfigPath(bundlePath);

      System.out.println();
      System.out.println(color("@|bold,cyan Testing bundle:|@ " + bundlePath));
      System.out.println();

      try {
        BundleLoader loader = new BundleLoader();
        JsltTransformEngine engine = new JsltTransformEngine();
        MappingBundle bundle = loader.load(configPath);

        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (Endpoint endpoint : bundle.endpoints()) {
          for (Step step : endpoint.steps()) {
            if (step.transform() != null && step.tests() != null) {
              Path transformPath = bundlePath.resolve(step.transform());

              for (TestCase testCase : step.tests()) {
                String testName =
                    shortenPath(step.transform()) + " ← " + shortenPath(testCase.input());
                try {
                  Path inputPath = bundlePath.resolve(testCase.input());
                  Path expectedPath = bundlePath.resolve(testCase.output());

                  JsonNode input = engine.loadJson(inputPath);
                  JsonNode expected = engine.loadJson(expectedPath);
                  JsonNode actual = engine.transform(transformPath, input);

                  if (expected.equals(actual)) {
                    System.out.println(color("  @|green ✓|@ " + testName));
                    passed++;
                  } else {
                    System.out.println(color("  @|red ✗|@ " + testName));
                    String detail = testName;
                    if (verbose) {
                      detail +=
                          "\n    Expected: "
                              + expected
                                  .toString()
                                  .substring(0, Math.min(100, expected.toString().length()))
                              + "\n    Actual:   "
                              + actual
                                  .toString()
                                  .substring(0, Math.min(100, actual.toString().length()));
                    }
                    failures.add(detail);
                    failed++;
                  }
                } catch (Exception e) {
                  System.out.println(color("  @|red ✗|@ " + testName + " - " + e.getMessage()));
                  failures.add(testName + "\n    Error: " + e.getMessage());
                  failed++;
                }
              }
            }
          }
        }

        System.out.println();
        if (failed == 0) {
          System.out.println(color("@|bold,green ✓ " + passed + " tests passed|@"));
        } else {
          System.out.println(
              color(
                  "@|bold Results:|@ @|green "
                      + passed
                      + " passed|@, @|red "
                      + failed
                      + " failed|@"));

          if (!failures.isEmpty() && verbose) {
            System.out.println();
            System.out.println(color("@|bold,red Failures:|@"));
            for (String failure : failures) {
              System.out.println("  " + failure);
            }
          }
        }
        System.out.println();

        return failed == 0 ? 0 : 1;

      } catch (Exception e) {
        System.err.println(color("@|bold,red ✗ Test failed:|@ " + e.getMessage()));
        return 1;
      }
    }

    private String shortenPath(String path) {
      int lastSlash = path.lastIndexOf('/');
      return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
  }

  @Command(
      name = "dry-run",
      description = "Transform input through steps without executing queries",
      mixinStandardHelpOptions = true)
  static class DryRunCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the bundle directory")
    private Path bundlePath;

    @Parameters(index = "1", description = "Input JSON file")
    private Path inputPath;

    @Option(
        names = {"-s", "--steps"},
        description = "Maximum number of steps to execute",
        defaultValue = "999")
    private int maxSteps;

    @Option(
        names = {"-e", "--endpoint"},
        description = "Endpoint index (default: 0)",
        defaultValue = "0")
    private int endpointIndex;

    @Override
    public Integer call() {
      bundlePath = bundlePath.toAbsolutePath();
      inputPath = inputPath.toAbsolutePath();
      Path configPath = resolveConfigPath(bundlePath);

      if (!Files.exists(inputPath)) {
        System.err.println(color("@|bold,red ✗ Input file not found:|@ " + inputPath));
        return 1;
      }

      System.out.println();
      System.out.println(color("@|bold,cyan Dry-run:|@ " + bundlePath));
      System.out.println();

      try {
        BundleLoader loader = new BundleLoader();
        JsltTransformEngine engine = new JsltTransformEngine();
        MappingBundle bundle = loader.load(configPath);

        if (bundle.endpoints().isEmpty()) {
          System.err.println(color("@|red Bundle has no endpoints|@"));
          return 1;
        }

        if (endpointIndex >= bundle.endpoints().size()) {
          System.err.println(color("@|red Endpoint index out of range|@"));
          return 1;
        }

        Endpoint endpoint = bundle.endpoints().get(endpointIndex);
        System.out.println(color("@|yellow Endpoint:|@ " + endpoint.path()));
        System.out.println();

        JsonNode current = engine.loadJson(inputPath);

        System.out.println(color("@|bold Input:|@"));
        System.out.println(current.toPrettyString());

        int stepIndex = 0;
        for (Step step : endpoint.steps()) {
          if (stepIndex >= maxSteps) break;

          System.out.println();
          if (step.transform() != null) {
            Path transformPath = bundlePath.resolve(step.transform());
            current = engine.transform(transformPath, current);
            System.out.println(
                color("@|bold Step " + stepIndex + "|@ @|green (transform)|@ " + step.transform()));
            System.out.println(current.toPrettyString());
          } else if (step.query() != null) {
            System.out.println(
                color(
                    "@|bold Step "
                        + stepIndex
                        + "|@ @|yellow (query)|@ "
                        + step.query()
                        + " @|faint [skipped]|@"));
          }

          stepIndex++;
        }

        System.out.println();
        return 0;

      } catch (Exception e) {
        System.err.println(color("@|bold,red ✗ Dry-run failed:|@ " + e.getMessage()));
        return 1;
      }
    }
  }

  @Command(
      name = "e2e",
      description = "Run end-to-end tests against a remote MOLGENIS server",
      mixinStandardHelpOptions = true)
  static class E2eCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the bundle directory")
    private Path bundlePath;

    @Option(
        names = {"-s", "--server"},
        description = "MOLGENIS server URL",
        defaultValue = "${MOLGENIS_SERVER}")
    private String server;

    @Option(
        names = {"-t", "--token"},
        description = "API token for authentication",
        defaultValue = "${MOLGENIS_TOKEN}")
    private String token;

    @Option(
        names = {"--schema"},
        description = "Schema name (overrides e2e config)")
    private String schema;

    @Override
    public Integer call() {
      System.out.println();
      System.out.println(color("@|bold,cyan E2e tests:|@ " + bundlePath));
      System.out.println();

      if (server == null || server.isBlank()) {
        System.err.println(color("@|red Server required. Use --server or set MOLGENIS_SERVER|@"));
        return 1;
      }

      System.out.println(color("  @|bold Server:|@ " + server));
      System.out.println(color("  @|bold Token:|@  " + (token != null ? "***" : "(none)")));
      System.out.println();
      System.err.println(color("@|yellow E2e execution not yet implemented.|@"));
      System.err.println("Will execute GraphQL queries against remote server.");
      System.out.println();

      return 2;
    }
  }

  private static String color(String text) {
    return CommandLine.Help.Ansi.AUTO.string(text);
  }
}

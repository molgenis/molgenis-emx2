package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.molgenis.emx2.fairmapper.executor.FetchExecutor;
import org.molgenis.emx2.fairmapper.model.E2eTestCase;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.Step;
import org.molgenis.emx2.fairmapper.model.TestCase;
import org.molgenis.emx2.fairmapper.model.step.FetchStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;
import org.molgenis.emx2.fairmapper.rdf.LocalRdfSource;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
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
      RunFairMapper.E2eCommand.class,
      RunFairMapper.FetchRdfCommand.class,
      RunFairMapper.RunCommand.class
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
        System.out.println(color("  @|bold Mappings:|@ " + bundle.getMappings().size()));
        System.out.println();

        for (Mapping mapping : bundle.getMappings()) {
          String displayPath =
              mapping.endpoint() != null ? mapping.endpoint() : mapping.getEffectiveName();
          System.out.println(
              color(
                  "  @|yellow "
                      + displayPath
                      + "|@ ["
                      + String.join(", ", mapping.methods())
                      + "]"));
          System.out.println("    Steps: " + mapping.steps().size());

          int testCount = countMappingStepTests(mapping);
          if (testCount > 0) {
            System.out.println("    Unit tests: " + testCount);
          }

          if (mapping.e2e() != null && mapping.e2e().tests() != null) {
            System.out.println("    E2e tests: " + mapping.e2e().tests().size());
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

    private int countMappingStepTests(Mapping mapping) {
      int count = 0;
      for (StepConfig step : mapping.steps()) {
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

        LocalRdfSource rdfSource = new LocalRdfSource(bundlePath);
        FetchExecutor fetchExecutor = new FetchExecutor(rdfSource, bundlePath);
        ObjectMapper objectMapper = new ObjectMapper();

        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (Mapping mapping : bundle.getMappings()) {
          for (StepConfig step : mapping.steps()) {
            if (step instanceof TransformStep transformStep && transformStep.tests() != null) {
              Path transformPath = bundlePath.resolve(transformStep.path());

              for (TestCase testCase : transformStep.tests()) {
                String testName =
                    shortenPath(transformStep.path()) + " ← " + shortenPath(testCase.input());
                try {
                  Path inputPath = bundlePath.resolve(testCase.input());
                  Path expectedPath = bundlePath.resolve(testCase.output());

                  JsonNode input = engine.loadJson(inputPath);
                  JsonNode expected = engine.loadJson(expectedPath);
                  JsonNode actual = engine.transform(transformPath, input);

                  if (jsonEquals(expected, actual)) {
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
            } else if (step instanceof FetchStep fetchStep && fetchStep.tests() != null) {
              for (TestCase testCase : fetchStep.tests()) {
                String testName =
                    "fetch("
                        + shortenPath(fetchStep.url())
                        + ") ← "
                        + shortenPath(testCase.input());
                try {
                  Path expectedPath = bundlePath.resolve(testCase.output());
                  JsonNode expected = objectMapper.readTree(Files.readString(expectedPath));
                  JsonNode actual = fetchExecutor.execute(fetchStep, testCase.input());

                  if (jsonEquals(expected, actual)) {
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
                                  .substring(0, Math.min(200, expected.toString().length()))
                              + "\n    Actual:   "
                              + actual
                                  .toString()
                                  .substring(0, Math.min(200, actual.toString().length()));
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

    private boolean jsonEquals(JsonNode expected, JsonNode actual) {
      try {
        return JSONCompare.compareJSON(
                expected.toString(), actual.toString(), JSONCompareMode.NON_EXTENSIBLE)
            .passed();
      } catch (Exception e) {
        return false;
      }
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

        if (bundle.getMappings().isEmpty()) {
          System.err.println(color("@|red Bundle has no mappings|@"));
          return 1;
        }

        if (endpointIndex >= bundle.getMappings().size()) {
          System.err.println(color("@|red Mapping index out of range|@"));
          return 1;
        }

        Mapping mapping = bundle.getMappings().get(endpointIndex);
        String displayPath =
            mapping.endpoint() != null ? mapping.endpoint() : mapping.getEffectiveName();
        System.out.println(color("@|yellow Mapping:|@ " + displayPath));
        System.out.println();

        JsonNode current = engine.loadJson(inputPath);

        System.out.println(color("@|bold Input:|@"));
        System.out.println(current.toPrettyString());

        int stepIndex = 0;
        for (StepConfig step : mapping.steps()) {
          if (stepIndex >= maxSteps) break;

          System.out.println();
          if (step instanceof TransformStep transformStep) {
            Path transformPath = bundlePath.resolve(transformStep.path());
            current = engine.transform(transformPath, current);
            System.out.println(
                color(
                    "@|bold Step "
                        + stepIndex
                        + "|@ @|green (transform)|@ "
                        + transformStep.path()));
            System.out.println(current.toPrettyString());
          } else if (step instanceof org.molgenis.emx2.fairmapper.model.step.QueryStep queryStep) {
            System.out.println(
                color(
                    "@|bold Step "
                        + stepIndex
                        + "|@ @|yellow (query)|@ "
                        + queryStep.path()
                        + " @|faint [skipped]|@"));
          } else if (step instanceof FetchStep fetchStep) {
            System.out.println(
                color(
                    "@|bold Step "
                        + stepIndex
                        + "|@ @|cyan (fetch)|@ "
                        + fetchStep.url()
                        + " @|faint [skipped - use test command]|@"));
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

    @Option(
        names = {"-v", "--verbose"},
        description = "Show detailed output for each test")
    private boolean verbose;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Integer call() {
      bundlePath = bundlePath.toAbsolutePath();
      Path configPath = resolveConfigPath(bundlePath);

      System.out.println();
      System.out.println(color("@|bold,cyan E2e tests:|@ " + bundlePath));
      System.out.println();

      if (server == null || server.isBlank()) {
        System.err.println(color("@|red Server required. Use --server or set MOLGENIS_SERVER|@"));
        return 1;
      }

      System.out.println(color("@|bold Server:|@ " + server));
      System.out.println();

      try {
        BundleLoader loader = new BundleLoader();
        JsltTransformEngine transformEngine = new JsltTransformEngine();
        MappingBundle bundle = loader.load(configPath);

        GraphqlClient client = new GraphqlClient(server, token);

        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (Mapping mapping : bundle.getMappings()) {
          if (mapping.e2e() == null || mapping.e2e().tests() == null) {
            continue;
          }

          String effectiveSchema =
              (this.schema != null && !this.schema.isBlank())
                  ? this.schema
                  : mapping.e2e().schema();

          if (effectiveSchema == null || effectiveSchema.isBlank()) {
            String displayPath =
                mapping.endpoint() != null ? mapping.endpoint() : mapping.getEffectiveName();
            System.out.println(
                color("@|yellow " + displayPath + "|@ @|red (skipped - no schema configured)|@"));
            continue;
          }

          String displayPath =
              mapping.endpoint() != null ? mapping.endpoint() : mapping.getEffectiveName();
          System.out.println(
              color("@|yellow " + displayPath + "|@ (schema: " + effectiveSchema + ")"));

          RemotePipelineExecutor executor =
              new RemotePipelineExecutor(client, transformEngine, bundlePath, effectiveSchema);

          for (E2eTestCase testCase : mapping.e2e().tests()) {
            String testName = testCase.method() + " " + shortenPath(testCase.input());
            try {
              JsonNode input = loadJson(testCase.input());
              JsonNode expected = loadJson(testCase.output());
              JsonNode actual = executor.execute(input, mapping);

              if (jsonEquals(expected, actual)) {
                System.out.println(color("  @|green ✓|@ " + testName));
                passed++;
              } else {
                System.out.println(color("  @|red ✗|@ " + testName));
                String detail = testName;
                if (verbose) {
                  detail +=
                      "\n    Expected: "
                          + truncate(expected.toString(), 200)
                          + "\n    Actual:   "
                          + truncate(actual.toString(), 200);
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

        System.out.println();
        if (passed == 0 && failed == 0) {
          System.out.println(color("@|yellow No e2e tests found|@"));
        } else if (failed == 0) {
          System.out.println(
              color("@|bold,green ✓ " + passed + " test" + (passed == 1 ? "" : "s") + " passed|@"));
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
        System.err.println(color("@|bold,red ✗ E2e test failed:|@ " + e.getMessage()));
        return 1;
      }
    }

    private JsonNode loadJson(String relativePath) throws Exception {
      Path fullPath = bundlePath.resolve(relativePath).normalize();
      if (!Files.exists(fullPath)) {
        throw new Exception("File not found: " + relativePath);
      }
      return objectMapper.readTree(Files.readString(fullPath));
    }

    private boolean jsonEquals(JsonNode expected, JsonNode actual) {
      try {
        return JSONCompare.compareJSON(
                expected.toString(), actual.toString(), JSONCompareMode.NON_EXTENSIBLE)
            .passed();
      } catch (Exception e) {
        return false;
      }
    }

    private String shortenPath(String path) {
      int lastSlash = path.lastIndexOf('/');
      return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String truncate(String s, int maxLen) {
      return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
  }

  @Command(
      name = "fetch-rdf",
      description = "Fetch RDF from a URL and convert to JSON-LD",
      mixinStandardHelpOptions = true)
  static class FetchRdfCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "URL to fetch RDF from")
    private String url;

    @Option(
        names = {"-f", "--format"},
        description = "Output format: jsonld or turtle (default: jsonld)",
        defaultValue = "jsonld")
    private String format;

    @Option(
        names = {"--frame"},
        description = "Path to JSON-LD frame file for recursive link resolution")
    private Path framePath;

    @Option(
        names = {"--max-depth"},
        description = "Maximum depth for recursive fetching (default: 2)",
        defaultValue = "2")
    private int maxDepth;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Integer call() {
      System.err.println();
      System.err.println(color("@|bold,cyan Fetching RDF from:|@ " + url));
      if (framePath != null) {
        System.err.println(color("@|bold Frame:|@ " + framePath));
        System.err.println(color("@|bold Max depth:|@ " + maxDepth));
      }
      System.err.println();

      try {
        org.eclipse.rdf4j.model.Model model;

        if (framePath != null) {
          if (!Files.exists(framePath)) {
            System.err.println(color("@|bold,red ✗ Frame file not found:|@ " + framePath));
            return 1;
          }

          JsonNode frame = objectMapper.readTree(Files.readString(framePath));

          org.molgenis.emx2.fairmapper.rdf.RdfFetcher fetcher =
              new org.molgenis.emx2.fairmapper.rdf.RdfFetcher(url);
          org.molgenis.emx2.fairmapper.rdf.FrameAnalyzer analyzer =
              new org.molgenis.emx2.fairmapper.rdf.FrameAnalyzer();
          org.molgenis.emx2.fairmapper.rdf.FrameDrivenFetcher frameDrivenFetcher =
              new org.molgenis.emx2.fairmapper.rdf.FrameDrivenFetcher(fetcher, analyzer);

          model = frameDrivenFetcher.fetch(url, frame, maxDepth);

          System.err.println(color("@|green ✓ Fetched " + model.size() + " statements|@"));
          System.err.println();

          org.molgenis.emx2.fairmapper.rdf.JsonLdFramer framer =
              new org.molgenis.emx2.fairmapper.rdf.JsonLdFramer();
          JsonNode framedResult = framer.frame(model, frame);
          System.out.println(framedResult.toPrettyString());

        } else {
          org.molgenis.emx2.fairmapper.rdf.RdfFetcher fetcher =
              new org.molgenis.emx2.fairmapper.rdf.RdfFetcher(url);
          model = fetcher.fetch(url);

          System.err.println(color("@|green ✓ Fetched " + model.size() + " statements|@"));
          System.err.println();

          if ("turtle".equalsIgnoreCase(format)) {
            java.io.StringWriter writer = new java.io.StringWriter();
            org.eclipse.rdf4j.rio.Rio.write(model, writer, org.eclipse.rdf4j.rio.RDFFormat.TURTLE);
            System.out.println(writer.toString());
          } else {
            org.molgenis.emx2.fairmapper.rdf.RdfToJsonLd converter =
                new org.molgenis.emx2.fairmapper.rdf.RdfToJsonLd();
            String jsonLd = converter.convert(model);
            System.out.println(jsonLd);
          }
        }

        return 0;

      } catch (Exception e) {
        System.err.println(color("@|bold,red ✗ Fetch failed:|@ " + e.getMessage()));
        e.printStackTrace(System.err);
        return 1;
      }
    }
  }

  @Command(
      name = "run",
      description = "Execute a mapping against live data sources",
      mixinStandardHelpOptions = true)
  static class RunCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to FAIRmapper bundle directory")
    private Path bundlePath;

    @Parameters(index = "1", description = "Name of mapping to execute (from fairmapper.yaml)")
    private String mappingName;

    @Option(
        names = {"--source"},
        description = "URL to fetch data from (e.g. FDP catalog URL)",
        required = true)
    private String sourceUrl;

    @Option(
        names = {"-s", "--server"},
        description = "MOLGENIS server URL for mutations (env: MOLGENIS_SERVER)",
        defaultValue = "${MOLGENIS_SERVER}")
    private String server;

    @Option(
        names = {"--schema"},
        description = "Target schema for GraphQL mutations",
        required = true)
    private String schema;

    @Option(
        names = {"-t", "--token"},
        description = "MOLGENIS API token (env: MOLGENIS_TOKEN)",
        defaultValue = "${MOLGENIS_TOKEN}")
    private String token;

    @Option(
        names = {"--dry-run"},
        description = "Run fetch and transform steps, skip mutation, print result")
    private boolean dryRun;

    @Option(
        names = {"-v", "--verbose"},
        description = "Show step names and progress")
    private boolean verbose;

    @Option(
        names = {"--show-data"},
        description = "Print JSON data after each step (fetch, transform)")
    private boolean showData;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Integer call() {
      bundlePath = bundlePath.toAbsolutePath();
      Path configPath = resolveConfigPath(bundlePath);

      System.out.println();
      System.out.println(color("@|bold,cyan Running mapping:|@ " + mappingName));
      System.out.println(color("@|bold Source:|@ " + sourceUrl));
      System.out.println(color("@|bold Schema:|@ " + schema));
      if (dryRun) {
        System.out.println(color("@|yellow Mode:|@ dry-run (no mutations)"));
      }
      System.out.println();

      try {
        BundleLoader loader = new BundleLoader();
        MappingBundle bundle = loader.load(configPath);

        Mapping mapping =
            bundle.getMappings().stream()
                .filter(m -> mappingName.equals(m.name()) || mappingName.equals(m.endpoint()))
                .findFirst()
                .orElseThrow(() -> new Exception("Mapping not found: " + mappingName));

        if (verbose) {
          System.out.println(color("@|bold Steps:|@ " + mapping.steps().size()));
          System.out.println();
        }

        JsonNode current = null;
        int stepIndex = 0;

        for (StepConfig step : mapping.steps()) {
          if (step instanceof FetchStep fetchStep) {
            if (verbose) {
              System.out.println(
                  color("@|bold Step " + stepIndex + ":|@ @|cyan fetch|@ " + sourceUrl));
            }
            org.molgenis.emx2.fairmapper.rdf.RdfFetcher rdfFetcher =
                new org.molgenis.emx2.fairmapper.rdf.RdfFetcher(sourceUrl);
            FetchExecutor fetchExecutor = new FetchExecutor(rdfFetcher, bundlePath);
            current = fetchExecutor.execute(fetchStep, sourceUrl);
            if (verbose) {
              System.out.println(color("@|green ✓|@ Fetched data"));
            }
            if (showData) {
              System.out.println(current.toPrettyString());
              System.out.println();
            }
          } else if (step instanceof TransformStep transformStep) {
            if (verbose) {
              System.out.println(
                  color(
                      "@|bold Step "
                          + stepIndex
                          + ":|@ @|green transform|@ "
                          + transformStep.path()));
            }
            JsltTransformEngine engine = new JsltTransformEngine();
            Path transformPath = bundlePath.resolve(transformStep.path());
            current = engine.transform(transformPath, current);
            if (verbose) {
              System.out.println(color("@|green ✓|@ Transformed data"));
            }
            if (showData) {
              System.out.println(current.toPrettyString());
              System.out.println();
            }
          } else if (step
              instanceof org.molgenis.emx2.fairmapper.model.step.MutateStep mutateStep) {
            if (verbose) {
              System.out.println(
                  color(
                      "@|bold Step " + stepIndex + ":|@ @|magenta mutate|@ " + mutateStep.path()));
            }
            if (dryRun) {
              System.out.println();
              System.out.println(color("@|bold,yellow Dry-run output (mutation skipped):|@"));
              System.out.println(current.toPrettyString());
              System.out.println();
              System.out.println(color("@|yellow Mutation file:|@ " + mutateStep.path()));
              return 0;
            } else {
              if (server == null || server.isBlank()) {
                System.err.println(
                    color(
                        "@|red Server required for mutations. Use --server or set MOLGENIS_SERVER|@"));
                return 1;
              }
              GraphqlClient client = new GraphqlClient(server, token);
              Path mutatePath = bundlePath.resolve(mutateStep.path());
              String mutation = Files.readString(mutatePath);
              current = client.execute(schema, mutation, current);
              if (verbose) {
                System.out.println(color("@|green ✓|@ Mutation executed"));
              }
            }
          }
          stepIndex++;
        }

        System.out.println();
        if (!dryRun) {
          System.out.println(color("@|bold,green ✓ Pipeline completed successfully|@"));
          System.out.println();
          System.out.println(color("@|bold Result:|@"));
          System.out.println(current.toPrettyString());
        }
        System.out.println();

        return 0;

      } catch (Exception e) {
        System.err.println();
        System.err.println(color("@|bold,red ✗ Execution failed:|@ " + e.getMessage()));
        if (verbose) {
          e.printStackTrace(System.err);
        }
        System.err.println();
        return 1;
      }
    }
  }

  private static String color(String text) {
    return CommandLine.Help.Ansi.AUTO.string(text);
  }
}

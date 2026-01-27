package org.molgenis.emx2.fairmapper.commands;

import static org.molgenis.emx2.fairmapper.RunFairMapper.color;
import static org.molgenis.emx2.fairmapper.RunFairMapper.resolveConfigPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.molgenis.emx2.fairmapper.BundleLoader;
import org.molgenis.emx2.fairmapper.GraphqlClient;
import org.molgenis.emx2.fairmapper.JsltTransformEngine;
import org.molgenis.emx2.fairmapper.executor.FetchExecutor;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.step.FetchStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;
import picocli.CommandLine.*;

@Command(
    name = "run",
    description = "Execute a mapping against live data sources",
    mixinStandardHelpOptions = true)
public class RunCommand implements Callable<Integer> {

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
        } else if (step instanceof org.molgenis.emx2.fairmapper.model.step.MutateStep mutateStep) {
          if (verbose) {
            System.out.println(
                color("@|bold Step " + stepIndex + ":|@ @|magenta mutate|@ " + mutateStep.path()));
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

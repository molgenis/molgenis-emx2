package org.molgenis.emx2.fairmapper.commands;

import static org.molgenis.emx2.fairmapper.RunFairMapper.color;
import static org.molgenis.emx2.fairmapper.RunFairMapper.resolveConfigPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.molgenis.emx2.fairmapper.BundleLoader;
import org.molgenis.emx2.fairmapper.JsltTransformEngine;
import org.molgenis.emx2.fairmapper.executor.FetchExecutor;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.TestCase;
import org.molgenis.emx2.fairmapper.model.step.FetchStep;
import org.molgenis.emx2.fairmapper.model.step.OutputRdfStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;
import org.molgenis.emx2.fairmapper.rdf.JsonLdToRdf;
import org.molgenis.emx2.fairmapper.rdf.LocalRdfSource;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import picocli.CommandLine.*;

@Command(
    name = "test",
    description = "Run step-level unit tests (transforms)",
    mixinStandardHelpOptions = true)
public class TestCommand implements Callable<Integer> {

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
                  "fetch(" + shortenPath(fetchStep.url()) + ") ← " + shortenPath(testCase.input());
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
          } else if (step instanceof OutputRdfStep rdfStep && rdfStep.tests() != null) {
            JsonLdToRdf converter = new JsonLdToRdf();

            for (TestCase testCase : rdfStep.tests()) {
              String testName =
                  "rdf(" + rdfStep.defaultFormat() + ") ← " + shortenPath(testCase.input());
              try {
                Path inputPath = bundlePath.resolve(testCase.input());
                Path expectedPath = bundlePath.resolve(testCase.output());

                JsonNode input = objectMapper.readTree(Files.readString(inputPath));
                String expected = Files.readString(expectedPath).trim();
                String actual = converter.convert(input.toString(), rdfStep.defaultFormat()).trim();

                if (expected.equals(actual)) {
                  System.out.println(color("  @|green ✓|@ " + testName));
                  passed++;
                } else {
                  System.out.println(color("  @|red ✗|@ " + testName));
                  String detail = testName;
                  if (verbose) {
                    detail +=
                        "\n    Expected: "
                            + expected.substring(0, Math.min(200, expected.length()))
                            + "\n    Actual:   "
                            + actual.substring(0, Math.min(200, actual.length()));
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

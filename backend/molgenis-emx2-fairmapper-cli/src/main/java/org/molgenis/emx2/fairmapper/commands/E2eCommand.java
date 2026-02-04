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
import org.molgenis.emx2.fairmapper.GraphqlClient;
import org.molgenis.emx2.fairmapper.JsltTransformEngine;
import org.molgenis.emx2.fairmapper.RemotePipelineExecutor;
import org.molgenis.emx2.fairmapper.model.E2eTestCase;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import picocli.CommandLine.*;

@Command(
    name = "e2e",
    description = "Run end-to-end tests against a remote MOLGENIS server",
    mixinStandardHelpOptions = true)
public class E2eCommand implements Callable<Integer> {

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
            (this.schema != null && !this.schema.isBlank()) ? this.schema : mapping.e2e().schema();

        if (effectiveSchema == null || effectiveSchema.isBlank()) {
          String displayPath = mapping.route() != null ? mapping.route() : mapping.name();
          System.out.println(
              color("@|yellow " + displayPath + "|@ @|red (skipped - no schema configured)|@"));
          continue;
        }

        String displayPath = mapping.route() != null ? mapping.route() : mapping.name();
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
      System.err.println(color("@|red Error comparing JSON: " + e.getMessage() + "|@"));
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

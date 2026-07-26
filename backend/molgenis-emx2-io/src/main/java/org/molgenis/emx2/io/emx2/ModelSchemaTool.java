package org.molgenis.emx2.io.emx2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class ModelSchemaTool {

  private static final String MODE_GENERATE = "generate";
  private static final String MODE_CHECK_DRIFT = "check-drift";
  private static final String MODE_VALIDATE_FIXTURES = "validate-fixtures";

  private ModelSchemaTool() {}

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      fail(
          "usage: ModelSchemaTool <"
              + MODE_GENERATE
              + "|"
              + MODE_CHECK_DRIFT
              + "|"
              + MODE_VALIDATE_FIXTURES
              + "> ...");
    }
    switch (args[0]) {
      case MODE_GENERATE -> generate(Path.of(args[1]));
      case MODE_CHECK_DRIFT -> checkDrift(Path.of(args[1]));
      case MODE_VALIDATE_FIXTURES -> validateFixtures(Path.of(args[1]), Path.of(args[2]));
      default -> fail("unknown mode: " + args[0]);
    }
  }

  private static void generate(Path artifact) throws IOException {
    Files.writeString(artifact, ModelSchemaGenerator.generate(), StandardCharsets.UTF_8);
    System.out.println("wrote " + artifact);
  }

  private static void checkDrift(Path artifact) throws IOException {
    String generated = ModelSchemaGenerator.generate();
    if (!Files.exists(artifact)) {
      fail("published JSON Schema artifact is missing: " + artifact + "; run generateModelSchema");
    }
    String committed = Files.readString(artifact, StandardCharsets.UTF_8);
    if (!generated.equals(committed)) {
      fail(
          "published JSON Schema artifact is stale: "
              + artifact
              + " differs from the validator rules; run generateModelSchema and commit the result");
    }
    System.out.println("model JSON Schema artifact is current: " + artifact);
  }

  private static void validateFixtures(Path fixturesRoot, Path artifact) throws IOException {
    String schemaJson = Files.readString(artifact, StandardCharsets.UTF_8);
    ModelSchemaValidator schemaValidator = new ModelSchemaValidator(schemaJson);
    List<String> violations = new ArrayList<>();

    List<Path> yamlFiles = listYamlFiles(fixturesRoot);
    for (Path yamlFile : yamlFiles) {
      String label = fixturesRoot.relativize(yamlFile).toString();
      violations.addAll(schemaValidator.validate(Files.readString(yamlFile), label));
    }

    for (Path bundleDir : listBundleDirs(fixturesRoot)) {
      try {
        BundleValidator.validate(bundleDir);
      } catch (RuntimeException exception) {
        violations.add(fixturesRoot.relativize(bundleDir) + ": " + exception.getMessage());
      }
    }

    if (!violations.isEmpty()) {
      System.err.println("model fixture validation FAILED:");
      for (String violation : violations) {
        System.err.println("  - " + violation);
      }
      System.exit(1);
    }
    System.out.println(
        "validated "
            + yamlFiles.size()
            + " model files across "
            + listBundleDirs(fixturesRoot).size()
            + " fixture bundles");
  }

  private static List<Path> listYamlFiles(Path root) throws IOException {
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".yaml"))
          .sorted()
          .toList();
    }
  }

  private static List<Path> listBundleDirs(Path root) throws IOException {
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.filter(Files::isRegularFile)
          .filter(path -> Emx2Yaml.MOLGENIS_YAML.equals(path.getFileName().toString()))
          .map(Path::getParent)
          .sorted()
          .toList();
    }
  }

  private static void fail(String message) {
    System.err.println(message);
    System.exit(1);
  }
}

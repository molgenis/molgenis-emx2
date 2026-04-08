package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.emx2.SchemaMetadata;

class AllBundlesIntegrationTest {

  private static final Path PROFILES_DIR = findProfilesPath();
  private static final Path DATA_DIR = PROFILES_DIR.getParent().resolve("data");

  private static Path findProfilesPath() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve("profiles");
      if (candidate.toFile().isDirectory()) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException(
        "Could not find profiles directory from: " + Path.of("").toAbsolutePath());
  }

  static Stream<Arguments> discoverAllBundles() throws IOException {
    List<Arguments> bundles = new ArrayList<>();

    // Single-file bundles: *.yaml directly under profiles/
    try (Stream<Path> entries = Files.list(PROFILES_DIR)) {
      entries
          .filter(p -> p.toString().endsWith(".yaml"))
          .forEach(
              p -> bundles.add(Arguments.of(p.getFileName().toString().replace(".yaml", ""), p)));
    }

    // Directory bundles: subdirectories containing molgenis.yaml
    try (Stream<Path> entries = Files.list(PROFILES_DIR)) {
      entries
          .filter(Files::isDirectory)
          .filter(d -> d.resolve("molgenis.yaml").toFile().exists())
          .forEach(d -> bundles.add(Arguments.of(d.getFileName().toString(), d)));
    }

    return bundles.stream();
  }

  static Stream<Arguments> discoverBundlesWithDemodata() throws IOException {
    List<Arguments> bundles = new ArrayList<>();

    for (Arguments args : (Iterable<Arguments>) discoverAllBundles()::iterator) {
      Object[] objs = args.get();
      String bundleName = (String) objs[0];
      Path bundlePath = (Path) objs[1];
      try {
        Emx2Yaml.BundleResult result = Emx2Yaml.fromBundle(bundlePath);
        List<String> demodata = result.getBundle().demodata();
        if (demodata != null && !demodata.isEmpty()) {
          bundles.add(Arguments.of(bundleName, bundlePath, demodata));
        }
      } catch (Exception e) {
        // bundles that fail to parse are caught by the other test
      }
    }

    return bundles.stream();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("discoverAllBundles")
  void bundleParsesSuccessfully(String bundleName, Path bundlePath) throws IOException {
    Emx2Yaml.BundleResult result = Emx2Yaml.fromBundle(bundlePath);

    assertNotNull(result.getName(), "Bundle must have a name");
    assertFalse(result.getName().isBlank(), "Bundle name must not be blank");

    SchemaMetadata schema = result.getSchema();
    assertFalse(schema.getTableNames().isEmpty(), "Bundle must define at least one table");

    System.out.printf("Bundle '%s': %d tables%n", result.getName(), schema.getTableNames().size());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("discoverBundlesWithDemodata")
  void demodataPathsResolveAndContainData(
      String bundleName, Path bundlePath, List<String> demodataPaths) throws IOException {
    for (String demodataPath : demodataPaths) {
      Path resolvedDir = resolveDemodataPath(demodataPath);
      assertTrue(Files.exists(resolvedDir), "Demodata directory should exist: " + resolvedDir);

      long csvCount;
      long totalRows;
      try (Stream<Path> files = Files.walk(resolvedDir)) {
        List<Path> csvFiles = files.filter(p -> p.toString().endsWith(".csv")).toList();

        csvCount = csvFiles.size();
        totalRows =
            csvFiles.stream()
                .mapToLong(
                    csv -> {
                      try {
                        long lines = Files.lines(csv).count();
                        return lines > 0 ? lines - 1 : 0; // subtract header
                      } catch (IOException e) {
                        return 0;
                      }
                    })
                .sum();
      }

      assertTrue(
          csvCount > 0, "Demodata path '" + demodataPath + "' must contain at least 1 CSV file");
      System.out.printf(
          "Bundle '%s' demodata '%s': %d CSV files, %d total rows%n",
          bundleName, demodataPath, csvCount, totalRows);
    }
  }

  private Path resolveDemodataPath(String demodataPath) {
    // Try direct resolution under data/ first (e.g. biobank-directory/demo)
    Path direct = DATA_DIR.resolve(demodataPath);
    if (Files.exists(direct)) {
      return direct;
    }
    // Paths starting with _ are directly under data/ (e.g. _demodata/applications/petstore)
    return DATA_DIR.resolve(demodataPath);
  }
}

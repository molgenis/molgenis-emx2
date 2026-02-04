package org.molgenis.emx2.fairmapper.commands;

import static org.molgenis.emx2.fairmapper.RunFairMapper.color;
import static org.molgenis.emx2.fairmapper.RunFairMapper.resolveConfigPath;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.molgenis.emx2.fairmapper.BundleLoader;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import picocli.CommandLine.*;

@Command(
    name = "validate",
    description = "Check FAIRmapper bundle structure and fairmapper.yaml file existence",
    mixinStandardHelpOptions = true)
public class ValidateCommand implements Callable<Integer> {

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
        String displayPath = mapping.route() != null ? mapping.route() : mapping.name();
        System.out.println(
            color(
                "  @|yellow " + displayPath + "|@ [" + String.join(", ", mapping.methods()) + "]"));
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

package org.molgenis.emx2.fairmapper.commands;

import static org.molgenis.emx2.fairmapper.RunFairMapper.color;
import static org.molgenis.emx2.fairmapper.RunFairMapper.resolveConfigPath;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.molgenis.emx2.fairmapper.BundleLoader;
import org.molgenis.emx2.fairmapper.JsltTransformEngine;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.model.step.FetchStep;
import org.molgenis.emx2.fairmapper.model.step.OutputRdfStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;
import org.molgenis.emx2.fairmapper.rdf.JsonLdToRdf;
import picocli.CommandLine.*;

@Command(
    name = "dry-run",
    description = "Transform input through steps without executing queries",
    mixinStandardHelpOptions = true)
public class DryRunCommand implements Callable<Integer> {

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
                  "@|bold Step " + stepIndex + "|@ @|green (transform)|@ " + transformStep.path()));
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
        } else if (step instanceof OutputRdfStep rdfStep) {
          JsonLdToRdf converter = new JsonLdToRdf();
          String rdfOutput = converter.convert(current.toString(), rdfStep.defaultFormat());
          System.out.println(
              color("@|bold Step " + stepIndex + "|@ @|blue (rdf)|@ " + rdfStep.defaultFormat()));
          System.out.println(rdfOutput);
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

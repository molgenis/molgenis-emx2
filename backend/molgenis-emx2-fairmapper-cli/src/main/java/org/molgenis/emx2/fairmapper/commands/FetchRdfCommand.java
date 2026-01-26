package org.molgenis.emx2.fairmapper.commands;

import static org.molgenis.emx2.fairmapper.RunFairMapper.color;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.*;

@Command(
    name = "fetch-rdf",
    description = "Fetch RDF from a URL and convert to JSON-LD",
    mixinStandardHelpOptions = true)
public class FetchRdfCommand implements Callable<Integer> {

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

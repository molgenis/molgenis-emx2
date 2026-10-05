package org.molgenis.emx2.fairmapper.cli.commands;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.extractors.CrawlingRdfExtractor;
import org.molgenis.emx2.fairmapper.extractors.RdfExtractor;
import org.molgenis.emx2.fairmapper.pipeline.definition.ExtractSpec;
import org.molgenis.emx2.fairmapper.pipeline.definition.PipelineFile;
import picocli.CommandLine;

@CommandLine.Command(
    name = "extract",
    description =
        """
        Extract RDF from an FDP endpoint, or run only the extract step of a pipeline file
        """,
    mixinStandardHelpOptions = true)
public class Extract implements Runnable {

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  private Source source;

  static class Source {

    @CommandLine.Option(
        names = {"-r", "--rdf"},
        required = true,
        description = "FDP endpoint to extract RDF from")
    private String rdf;

    @CommandLine.Option(
        names = {"-c", "--config"},
        required = true,
        description = "Pipeline file (YAML) whose extract step to run")
    private Path config;
  }

  @CommandLine.Option(
      names = {"-o", "--output"},
      description = "Write results to specified path")
  private String outputPath;

  @SuppressWarnings("java:S2589")
  @Override
  public void run() {
    Repository repository = null;
    try {
      URI endpoint;
      RdfExtractor extractor;
      if (source.config != null) {
        ExtractSpec extract = PipelineFile.read(source.config).extract();
        endpoint = extract.url();
        extractor = extract.create();
      } else {
        endpoint = URI.create(source.rdf);
        extractor = new CrawlingRdfExtractor();
      }

      repository = new SailRepository(new NativeStore());
      extractor.addRdfToRepository(repository, endpoint);
      try (RepositoryConnection connection = repository.getConnection();
          FileOutputStream fos = new FileOutputStream(outputPath)) {
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, fos);
        connection.export(writer);
        writer.endRDF();
      } catch (IOException e) {
        throw new MolgenisException("Something went wrong extracting endpoint: " + endpoint, e);
      }
    } finally {
      if (repository != null && repository.isInitialized()) {
        repository.shutDown();
      }
    }
  }
}

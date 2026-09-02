package org.molgenis.emx2.fairmapper.cli.commands;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
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
import picocli.CommandLine;

@CommandLine.Command(
    name = "extract",
    description =
        """
        Extract RDF from provided endpoint
        """,
    mixinStandardHelpOptions = true)
public class Extract implements Runnable {

  @CommandLine.Option(
      names = {"-r", "--rdf"},
      required = true,
      description = "FDP endpoint to harvest")
  private String rdf;

  @CommandLine.Option(
      names = {"-o", "--output"},
      description = "Write results to specified path")
  private String outputPath;

  @Override
  public void run() {
    Repository repository = new SailRepository(new NativeStore());
    URI endpoint = URI.create(rdf);
    RdfExtractor extractor = new CrawlingRdfExtractor();
    extractor.addRdfToRepository(repository, endpoint);
    try (RepositoryConnection connection = repository.getConnection();
        FileOutputStream fos = new FileOutputStream(outputPath)) {
      RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, fos);
      connection.export(writer);
      writer.endRDF();
    } catch (IOException e) {
      throw new MolgenisException("Something went wrong extracting endpoint: " + rdf, e);
    }
  }
}

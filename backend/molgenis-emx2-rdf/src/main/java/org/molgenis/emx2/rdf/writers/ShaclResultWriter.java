package org.molgenis.emx2.rdf.writers;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaclResultWriter extends RdfWriter {
  private static final byte[] SHACL_SUCCEED =
          """
    @prefix sh: <http://www.w3.org/ns/shacl#> .
    
    [] a sh:ValidationReport;
      sh:conforms true.
    """
                  .getBytes();

  private static final Logger logger = LoggerFactory.getLogger(ShaclResultWriter.class);
  private static final SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();

  private final SailRepository repository;
  private final SailRepositoryConnection connection;

  private int tripleCounter = 0;

  /**
   * @param shaclFiles shapes must be Turtle-formatted
   */
  public ShaclResultWriter(OutputStream outputStream, RDFFormat format, File... shaclFiles)
      throws IOException {
    super(outputStream, format);
    ShaclSail shaclSail = new ShaclSail(new MemoryStore());
    repository = new SailRepository(shaclSail);
    repository.init();
    connection = repository.getConnection();

    for (File file : shaclFiles) {
      addRules(file);
    }

    // Connection beginning for adding triples through a generator
    connection.begin();
  }

  private void addRules(File file) throws IOException {
    connection.begin();
    connection.add(file, null, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
    connection.commit();
  }

  @Override
  public void processNamespace(Namespace namespace) {}

  @Override
  public void processTriple(Statement statement) {
    connection.add(statement);
    tripleCounter++;
    // MemoryStore is designed for < 100.000 triples.
    if (tripleCounter == 100000) logger.warn("Exceeding supported number of triples for validation");
  }

  @Override
  public void processTriple(Resource subject, IRI predicate, Value object) {
    processTriple(valueFactory.createStatement(subject, predicate, object));
  }

  @Override
  public void close() {
    try {
      connection.commit();
      try {
        getOutputStream().write(SHACL_SUCCEED);
      } catch (IOException e) {
        throw new MolgenisException("An error occurred while writing the SHACL results");
      }
    } catch (RepositoryException e) {
      Throwable cause = e.getCause();
      if (cause instanceof ValidationException) {
        Model validationReportModel = ((ValidationException) cause).validationReportAsModel();
        Rio.write(validationReportModel, getOutputStream(), getFormat());
      } else {
        throw new MolgenisException("An error occurred during SHACL validation: " + e.getMessage());
      }
    } finally {
      connection.close();
      repository.shutDown();
    }
  }
}

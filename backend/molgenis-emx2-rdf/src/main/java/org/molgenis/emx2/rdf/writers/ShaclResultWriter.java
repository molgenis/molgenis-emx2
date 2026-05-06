package org.molgenis.emx2.rdf.writers;

import static org.eclipse.rdf4j.rio.helpers.BasicWriterSettings.INLINE_BLANK_NODES;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.rdf.shacl.ShaclSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaclResultWriter extends RdfOutputStreamWriter {
  private static final Logger logger = LoggerFactory.getLogger(ShaclResultWriter.class);
  private static final SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();
  private static final Model succeedModel;
  private static final WriterConfig succeedWriterConfig = new WriterConfig();

  static {
    ModelBuilder builder = new ModelBuilder();
    builder.setNamespace(SHACL.NS);
    BNode bNode = valueFactory.createBNode("");
    builder.add(bNode, RDF.TYPE, SHACL.VALIDATION_REPORT);
    builder.add(bNode, SHACL.CONFORMS, valueFactory.createLiteral(true));
    succeedModel = builder.build();

    succeedWriterConfig.set(INLINE_BLANK_NODES, true);
  }

  private final SailRepository repository;
  private final SailRepositoryConnection connection;

  public ShaclResultWriter(OutputStream outputStream, RDFFormat format, ShaclSet shaclSet)
      throws IOException {
    super(outputStream, format);
    ShaclSail shaclSail = new ShaclSail(new NativeStore());
    repository = new SailRepository(shaclSail);
    repository.init();
    connection = repository.getConnection();

    addRules(shaclSet);

    // Connection beginning for adding triples through a generator
    connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
  }

  private void addRules(ShaclSet shaclSet) throws IOException {
    logger.debug("Adding SHACL validation files: " + String.join(", ", shaclSet.files()));
    for (int i = 0; i < shaclSet.files().length; i++) {
      try (InputStream inputStream = shaclSet.getInputStream(i)) {
        connection.begin();
        connection.add(inputStream, null, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
        connection.commit();
      }
    }
  }

  @Override
  public void processNamespace(Namespace namespace) {}

  @Override
  public void processTriple(Statement statement) {
    connection.add(statement);
  }

  @Override
  public void processTriple(Resource subject, IRI predicate, Value object) {
    processTriple(valueFactory.createStatement(subject, predicate, object));
  }

  @Override
  public void close() {
    try {
      connection.commit();
      writeSuccess();
    } catch (RepositoryException e) {
      writeFailure(e);
    } finally {
      connection.close();
      repository.shutDown();
    }
  }

  private void writeSuccess() {
    try {
      Rio.write(succeedModel, getOutputStream(), getFormat(), succeedWriterConfig);
    } catch (RDFHandlerException e) {
      throw new MolgenisException("An error occurred while writing the SHACL results");
    }
  }

  private void writeFailure(RepositoryException e) {
    Throwable cause = e.getCause();
    if (cause instanceof ValidationException) {
      Model validationReportModel = ((ValidationException) cause).validationReportAsModel();
      Rio.write(validationReportModel, getOutputStream(), getFormat());
    } else {
      throw new MolgenisException("An error occurred during SHACL validation: " + e.getMessage());
    }
  }
}

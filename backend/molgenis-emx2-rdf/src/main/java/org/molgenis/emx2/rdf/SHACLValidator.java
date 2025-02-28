package org.molgenis.emx2.rdf;

import java.io.*;
import java.net.URL;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

/** Work in progress */
public class SHACLValidator {

  private final Repository repo;
  private static final IRI DATA_GRAPH =
      Values.iri("https://molgenis.org/Emx2RdfDataValidationGraph");

  /** Constructor to set up an in-memory RDF store */
  public SHACLValidator() {
    ShaclSail shaclSail = new ShaclSail(new MemoryStore());
    this.repo = new SailRepository(shaclSail);
  }

  /** Add and validate a SHACL shape to the shape graph by file location */
  public void addValidateShapesFromFile(String fileLocation) throws IOException {
    addValidateFromFile(fileLocation, RDF4J.SHACL_SHAPE_GRAPH);
  }

  /** Add and validate RDF data to the data graph by file location */
  public void addValidateDataFromFile(String fileLocation) throws IOException {
    addValidateFromFile(fileLocation, DATA_GRAPH);
  }

  /** Add and validate a SHACL shape to the shape graph by string input */
  public void addValidateShapesFromString(String rdf) throws IOException {
    addValidateFromString(rdf, RDF4J.SHACL_SHAPE_GRAPH);
  }

  /** Add and validate RDF data to the data graph by string input */
  public void addValidateDataFromString(String rdf) throws IOException {
    addValidateFromString(rdf, DATA_GRAPH);
  }

  /** Wrapper for addValidate to enable input by file */
  private void addValidateFromFile(String fileLocation, IRI targetGraph) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    URL url = classLoader.getResource(fileLocation);
    assert url != null;
    File file = new File(url.getFile());
    Reader rdfSource = new FileReader(file);
    addValidate(rdfSource, targetGraph);
  }

  /** Wrapper for addValidate to enable input by string */
  private void addValidateFromString(String rdf, IRI targetGraph) throws IOException {
    Reader rdfSource = new StringReader(rdf);
    addValidate(rdfSource, targetGraph);
  }

  /** Add the RDF data or SHACL to a target graph and validate */
  private void addValidate(Reader rdfSource, IRI targetGraph) throws IOException {
    RepositoryConnection connection = repo.getConnection();
    connection.begin();
    connection.add(rdfSource, "", RDFFormat.TURTLE, targetGraph);
    try {
      connection.commit();
    } catch (RepositoryException e) {
      if (e.getCause() instanceof ValidationException) {
        Model model = ((ValidationException) e.getCause()).validationReportAsModel();
        Rio.write(model, System.out, RDFFormat.TURTLE);
      }
      throw e;
    }
    connection.close();
  }

  /** Clear the shapes graph */
  public void clearShapes() {
    clearGraph(RDF4J.SHACL_SHAPE_GRAPH);
  }

  /** Clear the data graph */
  public void clearData() {
    clearGraph(DATA_GRAPH);
  }

  /** Clear the data graph and shapes graph */
  public void clearAll() {
    clearData();
    clearShapes();
  }

  /** Clear the target graph */
  private void clearGraph(IRI iri) {
    RepositoryConnection connection = repo.getConnection();
    connection.begin();
    connection.clear(iri);
    connection.commit();
    connection.close();
  }
}

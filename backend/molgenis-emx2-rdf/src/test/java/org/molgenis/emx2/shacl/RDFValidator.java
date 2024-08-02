package org.molgenis.emx2.shacl;

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
public class RDFValidator {

  /**
   * Example usage
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    RDFValidator rv = new RDFValidator();
    rv.addValidateShapes("shacl/fdp/1.2/metadata.ttl");
    rv.addValidateShapes("shacl/fdp/1.2/catalog.ttl");
    rv.addValidateData("emx2rdf/fdp.ttl");
    rv.clearAll();
  }

  private ShaclSail shaclSail;
  private Repository repo;
  private static final IRI DATA_GRAPH =
      Values.iri("https://molgenis.org/Emx2RdfDataValidationGraph");

  /** Constructor */
  public RDFValidator() {
    this.shaclSail = new ShaclSail(new MemoryStore());
    this.repo = new SailRepository(shaclSail);
  }

  public void addValidateShapes(String loc) throws IOException {
    addValidate(loc, RDF4J.SHACL_SHAPE_GRAPH);
  }

  public void addValidateData(String loc) throws IOException {
    addValidate(loc, DATA_GRAPH);
  }

  public void addValidate(String loc, IRI iri) throws IOException {
    RepositoryConnection connection = repo.getConnection();
    ClassLoader classLoader = getClass().getClassLoader();
    URL url = classLoader.getResource(loc);
    File shaclFile = new File(url.getFile());
    Reader shaclRules = new FileReader(shaclFile);
    connection.begin();
    connection.add(shaclRules, "", RDFFormat.TURTLE, iri);
    try {
      connection.commit();
    } catch (RepositoryException e) {
      if (e.getCause() instanceof ValidationException) {
        Model model = ((ValidationException) e.getCause()).validationReportAsModel();
        System.out.println("*** Report of failed graph validation ***");
        Rio.write(model, System.out, RDFFormat.TURTLE);
        System.out.println("*** end of report ***");
      }
    }
  }

  public void clearShapes() {
    clearGraph(RDF4J.SHACL_SHAPE_GRAPH);
  }

  public void clearData() {
    clearGraph(DATA_GRAPH);
  }

  public void clearAll() {
    clearData();
    clearShapes();
  }

  public void clearGraph(IRI iri) {
    RepositoryConnection connection = repo.getConnection();
    connection.begin();
    connection.clear(iri);
    connection.commit();
  }
}

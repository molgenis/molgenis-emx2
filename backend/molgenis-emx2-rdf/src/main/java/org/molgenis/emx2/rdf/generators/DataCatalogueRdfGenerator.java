package org.molgenis.emx2.rdf.generators;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.RdfMapData;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.rdf.writers.RdfWriter;

import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;
import static org.molgenis.emx2.rdf.IriGenerator.schemaIRI;

public class DataCatalogueRdfGenerator extends SemanticRdfGenerator {
  public DataCatalogueRdfGenerator(RdfWriter writer, String baseURL) {
    super(writer, baseURL);
  }

  @Override
  protected void dataRowToRdf(NamespaceMapper namespaces, RdfMapData rdfMapData, Table table, Row row) {
    super.dataRowToRdf(namespaces, rdfMapData, table, row);
    final IRI subject = rowIRI(getBaseURL(), table, row);
    getWriter()
      .processTriple(subject, DCAT.ENDPOINT_URL, schemaIRI(getBaseURL(), table.getSchema()));
  }
}

package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.RDFService.encodedIRI;

import java.net.URISyntaxException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableType;

public class TableToRDF {
  public static void describeTable(ModelBuilder builder, Table table, String schemaContext)
      throws URISyntaxException {
    IRI tableContext = encodedIRI(schemaContext + "/" + table.getName());
    builder.add(tableContext, RDF.TYPE, OWL.CLASS);
    builder.add(tableContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#DataSet"));
    builder.add(
        tableContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000754"));
    if (table.getMetadata().getSemantics() != null) {
      for (String tableSemantics : table.getMetadata().getSemantics()) {
        builder.add(tableContext, RDFS.ISDEFINEDBY, iri(tableSemantics));
      }
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(
          tableContext, RDFS.ISDEFINEDBY, iri("http://purl.obolibrary.org/obo/NCIT_C48697"));
    }
    builder.add(tableContext, RDFS.LABEL, table.getName());
    if (table.getMetadata().getTableType() == TableType.DATA) {
      builder.add(
          tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C25474"));
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(
          tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C21270"));
    }
  }
}

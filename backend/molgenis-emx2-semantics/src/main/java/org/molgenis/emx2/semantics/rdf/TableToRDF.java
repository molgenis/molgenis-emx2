package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.encodedIRI;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableType;

public class TableToRDF {
  public static void describeTable(ModelBuilder builder, Table table, String schemaContext) {
    IRI tableContext = encodedIRI(schemaContext + "/" + table.getName());
    builder.add(tableContext, RDF.TYPE, OWL.CLASS);
    builder.add(tableContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#DataSet"));
    // SIO:000754 = database table
    builder.add(tableContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000754"));
    if (table.getMetadata().getSemantics() != null) {
      for (String tableSemantics : table.getMetadata().getSemantics()) {
        builder.add(tableContext, RDFS.ISDEFINEDBY, iri(tableSemantics));
      }
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(
          // NCIT:C48697 = Controlled Vocabulary
          tableContext, RDFS.ISDEFINEDBY, iri("http://purl.obolibrary.org/obo/NCIT_C48697"));
    } else {
      builder.add(
          // SIO:001055 = observing (definition: observing is a process of passive interaction in
          // which one entity makes note of attributes of one or more entities)
          tableContext, RDFS.ISDEFINEDBY, iri("http://semanticscience.org/resource/SIO_001055"));
    }
    builder.add(tableContext, RDFS.LABEL, table.getName());
    if (table.getMetadata().getDescriptions() != null
        && table.getMetadata().getDescriptions().get("en") != null) {
      builder.add(
          tableContext, DCTERMS.DESCRIPTION, table.getMetadata().getDescriptions().get("en"));
    }
    if (table.getMetadata().getTableType() == TableType.DATA) {
      // NCIT:C25474 = Data
      builder.add(tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C25474"));
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      // NCIT:C21270 = Ontology
      builder.add(tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C21270"));
    }
  }
}

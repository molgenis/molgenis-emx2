package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.rdf.ColumnTypeToXSDDataType.columnTypeToXSD;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.encodedIRI;

import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Table;

public class ColumnToRDF {

  // todo: unit is missing (which would also be a sdmx-attribute:unitMeasure, typed as an
  // qb:AttributeProperty)
  public static void describeColumns(
      ModelBuilder builder, String columnName, Table table, String schemaContext) throws Exception {
    String tableContext = schemaContext + "/" + table.getName();
    for (Column c : table.getMetadata().getColumns()) {
      // allow selecting one particular column by ignoring the rest
      if (columnName != null && !c.getName().equals(columnName)) {
        continue;
      }
      String columnContext = tableContext + "/column/" + c.getName();
      // SIO:000757 = database column
      builder.add(columnContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000757"));
      builder.add(columnContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#MeasureProperty"));
      if (c.isReference() || c.isOntology()) {
        builder.add(columnContext, RDF.TYPE, OWL.OBJECTPROPERTY);
        builder.add(
            columnContext, RDFS.RANGE, encodedIRI(schemaContext + "/" + c.getRefTableName()));
      } else {
        builder.add(columnContext, RDF.TYPE, OWL.DATATYPEPROPERTY);
        builder.add(columnContext, RDFS.RANGE, columnTypeToXSD(c.getColumnType()));
      }
      builder.add(columnContext, RDFS.LABEL, c.getName());
      builder.add(columnContext, RDFS.DOMAIN, encodedIRI(tableContext));
      if (c.getSemantics() != null) {
        for (String columnSemantics : c.getSemantics()) {
          if (columnSemantics.equals("id")) {
            // fixme: need to figure out how to better handle 'id' tagging
            columnSemantics = "http://semanticscience.org/resource/SIO_000115";
          }
          builder.add(columnContext, RDFS.ISDEFINEDBY, iri(columnSemantics));
        }
      }
      if (c.getDescriptions() != null) {
        builder.add(columnContext, DC.DESCRIPTION, c.getDescriptions());
      }
    }
  }
}

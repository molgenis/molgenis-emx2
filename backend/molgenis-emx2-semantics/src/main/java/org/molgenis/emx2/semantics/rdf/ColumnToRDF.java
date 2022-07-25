package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.rdf.ColumnTypeToXSDDataType.columnTypeToXSD;

import org.eclipse.rdf4j.model.IRI;
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
  public static void describeColumns(ModelBuilder builder, Table table, IRI schemaContext)
      throws Exception {
    IRI tableContext = iri(schemaContext + "/" + table.getName());
    for (Column c : table.getMetadata().getColumns()) {
      IRI columnContext = iri(tableContext + "/column/" + c.getName());
      builder.add(columnContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000757"));
      // fixme: is isReference() same as isRef() ??
      if (c.isReference()) {
        builder.add(columnContext, RDF.TYPE, OWL.OBJECTPROPERTY);
      } else {
        builder.add(columnContext, RDF.TYPE, OWL.DATATYPEPROPERTY);
      }
      builder.add(columnContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#MeasureProperty"));
      builder.add(columnContext, RDFS.LABEL, c.getName());
      builder.add(columnContext, RDFS.DOMAIN, tableContext);
      if (c.getSemantics() != null) {
        for (String columnSemantics : c.getSemantics()) {
          builder.add(columnContext, RDFS.ISDEFINEDBY, iri(columnSemantics));
        }
      }
      if (c.getDescription() != null) {
        builder.add(columnContext, DC.DESCRIPTION, c.getDescription());
      }
      builder.add(columnContext, RDFS.RANGE, columnTypeToXSD(c.getColumnType()));
    }
  }
}

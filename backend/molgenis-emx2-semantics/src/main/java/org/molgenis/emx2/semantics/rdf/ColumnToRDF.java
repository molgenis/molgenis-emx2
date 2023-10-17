package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.rdf.ColumnTypeToXSDDataType.columnTypeToXSD;

import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Table;

public class ColumnToRDF {
  private ColumnToRDF() {
    // static only
  }

  // todo: unit is missing (which would also be a sdmx-attribute:unitMeasure, typed as an
  // qb:AttributeProperty)
  public static void describeColumns(
      ModelBuilder builder, String columnName, Table table, String schemaContext) {
    String tableContext = schemaContext + "/" + table.getName();
    for (Column column : table.getMetadata().getColumns()) {
      if (columnName == null || column.getName().equals(columnName)) {
        describeColumn(builder, schemaContext, column, tableContext);
      }
    }
  }

  private static void describeColumn(
      ModelBuilder builder, String schemaContext, Column column, String tableContext) {
    String columnContext = tableContext + "/column/" + column.getName();
    // SIO:000757 = database column
    builder.add(columnContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000757"));
    builder.add(columnContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#MeasureProperty"));
    if (column.isReference()) {
      builder.add(columnContext, RDF.TYPE, OWL.OBJECTPROPERTY);
      builder.add(
          columnContext,
          RDFS.RANGE,
          IRIParsingEncoding.encodedIRI(schemaContext + "/" + column.getRefTableName()));
    } else {
      builder.add(columnContext, RDF.TYPE, OWL.DATATYPEPROPERTY);
      builder.add(columnContext, RDFS.RANGE, columnTypeToXSD(column.getColumnType()));
    }
    builder.add(columnContext, RDFS.LABEL, column.getName());
    builder.add(columnContext, RDFS.DOMAIN, IRIParsingEncoding.encodedIRI(tableContext));
    if (column.getSemantics() != null) {
      for (String columnSemantics : column.getSemantics()) {
        if (columnSemantics.equals("id")) {
          // todo: need to figure out how to better handle 'id' tagging
          columnSemantics = "http://semanticscience.org/resource/SIO_000115";
        }
        builder.add(columnContext, RDFS.ISDEFINEDBY, iri(columnSemantics));
      }
    }
    if (column.getDescriptions() != null) {
      builder.add(columnContext, DC.DESCRIPTION, column.getDescriptions());
    }
  }
}

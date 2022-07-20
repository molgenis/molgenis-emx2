package org.molgenis.emx2.semantics;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.molgenis.emx2.ColumnType;

public class ColumnTypeToXSDDataType {
  public static IRI columnTypeToXSD(ColumnType columnType) throws Exception {
    switch (columnType) {
      case BOOL:
      case BOOL_ARRAY:
        return XSD.BOOLEAN;

      case DATE:
      case DATE_ARRAY:
        return XSD.DATE;

      case DATETIME:
      case DATETIME_ARRAY:
        return XSD.DATETIME;

      case DECIMAL:
      case DECIMAL_ARRAY:
        return XSD.DECIMAL;

      case EMAIL:
      case EMAIL_ARRAY:
      case HEADING:
      case JSONB:
      case JSONB_ARRAY:
      case STRING:
      case STRING_ARRAY:
      case TEXT:
      case TEXT_ARRAY:
      case UUID:
      case UUID_ARRAY:
        return XSD.STRING;

      case FILE:
      case HYPERLINK:
      case HYPERLINK_ARRAY:
      case ONTOLOGY:
      case ONTOLOGY_ARRAY:
      case REF:
      case REF_ARRAY:
      case REFBACK:
        return XSD.ANYURI;

      case INT:
      case INT_ARRAY:
        return XSD.INT;

      case LONG:
      case LONG_ARRAY:
        return XSD.LONG;

      default:
        throw new Exception("ColumnType not mapped: " + columnType);
    }
  }
}

package org.molgenis.emx2.semantics.rdf;

import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;

public class ColumnTypeToXSDDataType {
  public static CoreDatatype.XSD columnTypeToXSD(ColumnType columnType) {
    switch (columnType) {
      case BOOL:
      case BOOL_ARRAY:
        return CoreDatatype.XSD.BOOLEAN;

      case DATE:
      case DATE_ARRAY:
        return CoreDatatype.XSD.DATE;

      case DATETIME:
      case DATETIME_ARRAY:
        return CoreDatatype.XSD.DATETIME;

      case DECIMAL:
      case DECIMAL_ARRAY:
        return CoreDatatype.XSD.DECIMAL;

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
      case AUTO_ID:
        return CoreDatatype.XSD.STRING;

      case FILE:
      case HYPERLINK:
      case HYPERLINK_ARRAY:
      case ONTOLOGY:
      case ONTOLOGY_ARRAY:
      case REF:
      case REF_ARRAY:
      case REFBACK:
        return CoreDatatype.XSD.ANYURI;

      case INT:
      case INT_ARRAY:
        return CoreDatatype.XSD.INT;

      case LONG:
      case LONG_ARRAY:
        return CoreDatatype.XSD.LONG;

      default:
        throw new MolgenisException("ColumnType not mapped: " + columnType);
    }
  }
}

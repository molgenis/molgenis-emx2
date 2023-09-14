package org.molgenis.emx2.semantics.rdf;

import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.molgenis.emx2.ColumnType;

public class ColumnTypeToXSDDataType {
  public static CoreDatatype.XSD columnTypeToXSD(ColumnType columnType) throws Exception {
    return switch (columnType) {
      case BOOL, BOOL_ARRAY -> CoreDatatype.XSD.BOOLEAN;
      case DATE, DATE_ARRAY -> CoreDatatype.XSD.DATE;
      case DATETIME, DATETIME_ARRAY -> CoreDatatype.XSD.DATETIME;
      case DECIMAL, DECIMAL_ARRAY -> CoreDatatype.XSD.DECIMAL;
      case EMAIL,
          EMAIL_ARRAY,
          HEADING,
          JSONB,
          JSONB_ARRAY,
          STRING,
          STRING_ARRAY,
          TEXT,
          TEXT_ARRAY,
          UUID,
          UUID_ARRAY,
          AUTO_ID -> CoreDatatype.XSD.STRING;
      case FILE,
          HYPERLINK,
          HYPERLINK_ARRAY,
          ONTOLOGY,
          ONTOLOGY_ARRAY,
          REF,
          REF_ARRAY,
          REFBACK -> CoreDatatype.XSD.ANYURI;
      case INT, INT_ARRAY -> CoreDatatype.XSD.INT;
      case LONG, LONG_ARRAY -> CoreDatatype.XSD.LONG;
      default -> throw new Exception("ColumnType not mapped: " + columnType);
    };
  }
}

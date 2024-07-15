package org.molgenis.emx2.rdf;

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.rdf.RDFUtils.encodedIRI;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.molgenis.emx2.*;

public abstract class RDFService {
  static final String SETTING_SEMANTICS_REQUIRED = "semantics_required";

  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  public abstract String getMimeType();

  public abstract void describeAsRDF(
      final OutputStream outputStream,
      final Table table,
      final String rowId,
      final String columnName,
      final Schema... schemas);

  protected CoreDatatype.XSD columnTypeToXSD(final ColumnType columnType) {
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
      case PERIOD, PERIOD_ARRAY -> CoreDatatype.XSD.DURATION;
      default -> throw new MolgenisException("ColumnType not mapped: " + columnType);
    };
  }

  protected List<Value> getLiteralValues(final Row row, final Column column) {
    CoreDatatype.XSD xsdType = columnTypeToXSD(column.getColumnType());
    if (row.getString(column.getName()) == null) {
      return List.of();
    }
    return switch (xsdType) {
      case BOOLEAN -> Arrays.stream(row.getBooleanArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case DATE -> Arrays.stream(row.getDateArray(column.getName()))
          .map(value -> (Value) literal(value.toString(), xsdType))
          .toList();
      case DATETIME -> Arrays.stream(row.getDateTimeArray(column.getName()))
          .map(value -> (Value) literal(dateTimeFormatter.format(value), xsdType))
          .toList();
      case DECIMAL -> Arrays.stream(row.getDecimalArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case STRING -> Arrays.stream(row.getStringArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case ANYURI -> Arrays.stream(row.getStringArray(column.getName()))
          .map(value -> (Value) encodedIRI(value))
          .toList();
      case INT -> Arrays.stream(row.getIntegerArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case LONG -> Arrays.stream(row.getLongArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case DURATION -> Arrays.stream(row.getPeriodArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      default -> throw new MolgenisException("XSD type formatting not supported for: " + xsdType);
    };
  }
}

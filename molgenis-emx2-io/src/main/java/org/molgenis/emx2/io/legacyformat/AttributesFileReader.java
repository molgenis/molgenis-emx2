package org.molgenis.emx2.io.legacyformat;

import org.molgenis.Row;
import org.molgenis.emx2.io.csv.CsvRowReader;
import org.molgenis.Column;
import org.molgenis.SchemaMetadata;
import org.molgenis.TableMetadata;
import org.molgenis.Type;
import org.molgenis.utils.MolgenisException;
import org.molgenis.utils.MolgenisExceptionMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.*;
import static org.molgenis.emx2.io.legacyformat.AttributesFileHeader.*;

public class AttributesFileReader {

  public static final String PARSE_ERROR = "parse_error";
  public static final String PARSING_OF_ATTRIBUTES_FILE_FAILED =
      "Parsing of attributes file failed";

  public SchemaMetadata readModelFromCsv(File f) throws FileNotFoundException, MolgenisException {
    return convertAttributesToModel(readRowsFromCsv(new FileReader(f)));
  }

  public SchemaMetadata readModelFromCsv(Reader in) throws MolgenisException {
    return convertAttributesToModel(readRowsFromCsv(in));
  }

  public SchemaMetadata convertAttributesToModel(List<AttributesFileRow> rows)
      throws MolgenisException {
    SchemaMetadata model = new SchemaMetadata("test");

    int lineNumber = 0;
    List<MolgenisExceptionMessage> messages = new ArrayList<>();
    for (AttributesFileRow row : rows) {
      lineNumber++;
      convertAttributeLine(model, lineNumber, messages, row);
    }
    if (!messages.isEmpty())
      throw new MolgenisException("Attributes file reading failed", messages);
    return model;
  }

  private void convertAttributeLine(
      SchemaMetadata model,
      int lineNumber,
      List<MolgenisExceptionMessage> messages,
      AttributesFileRow row)
      throws MolgenisException {
    // get or create table
    TableMetadata table = model.getTableMetadata(row.getEntity());
    if (table == null) table = model.createTableIfNotExists(row.getEntity());

    // check if attribute exists
    if (table.getColumn(row.getName()) != null) {
      messages.add(
          new MolgenisExceptionMessage(
              lineNumber, "attribute " + row.getName() + " is defined twice"));
    } else {
      Type type = getEmxType(lineNumber, messages, row);
      Column column;
      if (REF.equals(type)) {
        column = table.addRef(row.getName(), row.getRefEntity(), MOLGENISID);
      } else {
        column = table.addColumn(row.getName(), type);
      }

      column.setNullable(row.getNillable());
      column.setDescription(row.getDescription());
      column.setReadonly(row.getReadonly());
    }
  }

  private Type getEmxType(
      int lineNumber, List<MolgenisExceptionMessage> messages, AttributesFileRow row) {
    Type type = STRING;
    if (row.getDataType() != null) {
      try {
        type = convertAttributeTypeToEmxType(row.getDataType());
      } catch (Exception e) {
        messages.add(new MolgenisExceptionMessage(lineNumber, e.getMessage()));
      }
    }
    return type;
  }

  private Type convertAttributeTypeToEmxType(String dataType) throws MolgenisException {
    try {
      AttributesType oldType = AttributesType.valueOf(dataType.toUpperCase());
      switch (oldType) {
        case STRING:
          return STRING;
        case INT:
          return INT;
        case DECIMAL:
          return DECIMAL;
        case TEXT:
          return TEXT;
        case BOOL:
          return BOOL;
        case DATE:
          return DATE;
        case DATETIME:
          return DATETIME;
        case XREF:
          return REF;
        case MREF:
          return MREF;
        case COMPOUND:
          throw new MolgenisException(
              PARSE_ERROR,
              PARSING_OF_ATTRIBUTES_FILE_FAILED,
              "new emx2format doesn't support 'compound' data type");
        case ONE_TO_MANY:
          throw new MolgenisException(
              PARSE_ERROR,
              PARSING_OF_ATTRIBUTES_FILE_FAILED,
              "new emx2format doesn't yet support 'ONE_TO_MANY' data type");
        default:
          throw new MolgenisException(
              PARSE_ERROR,
              PARSING_OF_ATTRIBUTES_FILE_FAILED,
              "new emx2format doesn't yet support " + oldType + " data type");
      }
    } catch (IllegalArgumentException e) {
      throw new MolgenisException(
          PARSE_ERROR,
          PARSING_OF_ATTRIBUTES_FILE_FAILED,
          "attributes type '" + dataType + "' not known");
    }
  }

  public List<AttributesFileRow> readRowsFromCsv(Reader in) throws MolgenisException {
    try {
      List<AttributesFileRow> rows = new ArrayList<>();
      for (Row record : CsvRowReader.read(in)) {
        AttributesFileRow row = new AttributesFileRow();
        row.setEntity(get(record, ENTITY));
        row.setName(get(record, NAME));
        row.setDataType(get(record, DATATYPE));
        row.setRefEntity(get(record, REFENTITY));
        row.setNillable(bool(get(record, NILLABLE)));
        row.setIdAttribute(bool(get(record, IDATTRIBUTE)));
        row.setDescription(get(record, DESCRIPTION));
        row.setRangeMin(integer(get(record, RANGEMIN)));
        row.setRangeMax(integer(get(record, RANGEMAX)));
        row.setLabel(get(record, LABEL));
        row.setAggregateable(bool(get(record, AGGREGATEABLE)));
        row.setLabelAttribute(bool(get(record, LABELATTRIBUTE)));
        row.setReadonly(bool(get(record, READONLY)));
        row.setValidationExepression(get(record, VALIDATIONEXPRESSION));
        row.setVisibleExpression(get(record, VISIBLE));
        row.setDefaultValue(get(record, DEFAULTVALUE));
        row.setPartOfAttribute(get(record, PARTOFATTRIBUTE));
        row.setExpression(get(record, EXPRESSION));
        rows.add(row);
      }
      return rows;
    } catch (IOException e) {
      throw new MolgenisException(PARSE_ERROR, "Parsing of attribuges file failed", e.getMessage());
    }
  }

  private String get(Row row, Enum<?> term) {
    try {
      String value = row.getString(term.toString().toLowerCase());
      if (value == null || "".equals(value.trim())) return null;
      return value;
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private Integer integer(String value) {
    if (value == null) return null;
    return Integer.parseInt(value);
  }

  private Boolean bool(String value) {
    return "TRUE".equalsIgnoreCase(value);
  }

  public List<AttributesFileRow> readRowsFromCsv(File f) throws MolgenisException {
    try {
      return readRowsFromCsv(new FileReader(f));
    } catch (IOException e) {
      throw new MolgenisException(e);
    }
  }
}

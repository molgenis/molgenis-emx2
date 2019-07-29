package org.molgenis.emx2.io.legacyformat;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.molgenis.*;
import org.molgenis.beans.SchemaBean;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.*;
import static org.molgenis.emx2.io.legacyformat.AttributesFileHeader.*;

public class AttributesFileReader {

  public Schema readModelFromCsv(File f) throws FileNotFoundException, MolgenisException {
    return convertAttributesToModel(readRowsFromCsv(new FileReader(f)));
  }

  public Schema readModelFromCsv(Reader in) throws MolgenisException {
    return convertAttributesToModel(readRowsFromCsv(in));
  }

  public Schema convertAttributesToModel(List<AttributesFileRow> rows) throws MolgenisException {
    Schema model = new SchemaBean("test");

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
      Schema model, int lineNumber, List<MolgenisExceptionMessage> messages, AttributesFileRow row)
      throws MolgenisException {
    // get or create table
    Table table = model.getTable(row.getEntity());
    if (table == null) table = model.createTable(row.getEntity());

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
      // column.setValidation(row.getValidationExepression());

      //      if (column.getType().equals(SELECT)
      //          || column.getType().equals(MSELECT)
      //          || column.getType().equals(RADIO)
      //          || column.getType().equals(CHECKBOX)) {
      //        refEntities.put(lineNumber, row.getRefEntity());
      //        refColumns.put(lineNumber, column);
      //      }
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
          //        case LONG:
          //          return LONG;
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
          //        case CATEGORICAL:
          //          return RADIO;
          //        case CATEGORICAL_MREF:
          //          return CHECKBOX;
        case COMPOUND:
          throw new MolgenisException("new format doesn't support 'compound' data type");
          //        case FILE:
          //          return FILE;
          //        case EMAIL:
          //          return EMAIL;
          //        case ENUM:
          //          return ENUM;
          //        case HYPERLINK:
          //          return HYPERLINK;
          //        case HTML:
          //          return HTML;
        case ONE_TO_MANY:
          throw new MolgenisException("new format doesn't yet support 'ONE_TO_MANY' data type");
        default:
          throw new MolgenisException("new format doesn't yet support " + oldType + " data type");
      }
    } catch (IllegalArgumentException e) {
      throw new MolgenisException("attributes type '" + dataType + "' not known");
    }
  }

  public List<AttributesFileRow> readRowsFromCsv(Reader in) throws MolgenisException {
    try {
      Iterable<CSVRecord> records =
          CSVFormat.DEFAULT
              .withHeader(AttributesType.class)
              .withAllowMissingColumnNames()
              .withFirstRecordAsHeader()
              .withIgnoreHeaderCase()
              .withIgnoreSurroundingSpaces()
              .withTrim()
              .withIgnoreEmptyLines(true)
              .parse(in);
      List<AttributesFileRow> rows = new ArrayList<>();
      for (CSVRecord record : records) {
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
      throw new MolgenisException(e.getMessage());
    }
  }

  private String get(CSVRecord record, Enum<?> term) {
    try {
      String value = record.get(term);
      if ("".equals(value.trim())) return null;
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

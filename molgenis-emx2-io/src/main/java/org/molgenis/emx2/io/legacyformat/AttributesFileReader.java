package org.molgenis.emx2.io.legacyformat;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.MolgenisReaderException;
import org.molgenis.emx2.io.MolgenisReaderMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.EmxType.*;
import static org.molgenis.emx2.io.legacyformat.AttributesFileHeader.*;

public class AttributesFileReader {

  public EmxModel readModelFromCsv(File f)
      throws MolgenisReaderException, FileNotFoundException, EmxException {
    return convertAttributesToModel(readRowsFromCsv(new FileReader(f)));
  }

  public EmxModel readModelFromCsv(Reader in) throws MolgenisReaderException, EmxException {
    return convertAttributesToModel(readRowsFromCsv(in));
  }

  public EmxModel convertAttributesToModel(List<AttributesFileRow> rows)
      throws MolgenisReaderException, EmxException {
    EmxModel model = new EmxModel();

    int lineNumber = 0;
    List<MolgenisReaderMessage> messages = new ArrayList<>();
    Map<Integer, String> refEntities = new LinkedHashMap<>();
    Map<Integer, EmxColumn> refColumns = new LinkedHashMap<>();
    for (AttributesFileRow row : rows) {
      lineNumber++;
      convertAttributeLine(model, lineNumber, messages, refEntities, refColumns, row);
    }
    // check and set refEntities
    for (Map.Entry<Integer, String> ref : refEntities.entrySet()) {
      EmxTable table = model.getTable(ref.getValue());
      if (table == null)
        messages.add(
            new MolgenisReaderMessage(
                ref.getKey(), "refEntity '" + ref.getValue() + "' is not known"));
      else refColumns.get(ref.getKey()).setRef(table.getIdColumn());
    }
    if (!messages.isEmpty()) throw new MolgenisReaderException(messages);
    return model;
  }

  private void convertAttributeLine(
      EmxModel model,
      int lineNumber,
      List<MolgenisReaderMessage> messages,
      Map<Integer, String> refEntities,
      Map<Integer, EmxColumn> refColumns,
      AttributesFileRow row)
      throws EmxException {
    // get or create table
    EmxTable table = model.getTable(row.getEntity());
    if (table == null) table = model.addTable(row.getEntity());

    // check if attribute exists
    if (table.getColumn(row.getName()) != null) {
      messages.add(
          new MolgenisReaderMessage(
              lineNumber, "attribute " + row.getName() + " is defined twice"));
    } else {
      EmxType type = getEmxType(lineNumber, messages, row);
      EmxColumn column = table.addColumn(row.getName(), type);

      column.setNillable(row.getNillable());
      column.setDescription(row.getDescription());
      column.setReadonly(row.getReadonly());
      column.setValidation(row.getValidationExepression());

      if (column.getType().equals(SELECT)
          || column.getType().equals(MSELECT)
          || column.getType().equals(RADIO)
          || column.getType().equals(CHECKBOX)) {
        refEntities.put(lineNumber, row.getRefEntity());
        refColumns.put(lineNumber, column);
      }
    }
  }

  private EmxType getEmxType(
      int lineNumber, List<MolgenisReaderMessage> messages, AttributesFileRow row) {
    EmxType type = STRING;
    if (row.getDataType() != null) {
      try {
        type = convertAttributeTypeToEmxType(row.getDataType());
      } catch (Exception e) {
        messages.add(new MolgenisReaderMessage(lineNumber, e.getMessage()));
      }
    }
    return type;
  }

  private EmxType convertAttributeTypeToEmxType(String dataType) throws MolgenisReaderException {
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
        case LONG:
          return LONG;
        case BOOL:
          return BOOL;
        case DATE:
          return DATE;
        case DATETIME:
          return DATETIME;
        case XREF:
          return SELECT;
        case MREF:
          return MSELECT;
        case CATEGORICAL:
          return RADIO;
        case CATEGORICAL_MREF:
          return CHECKBOX;
        case COMPOUND:
          throw new MolgenisReaderException("new format doesn't support 'compound' data type");
        case FILE:
          return FILE;
        case EMAIL:
          return EMAIL;
        case ENUM:
          return ENUM;
        case HYPERLINK:
          return HYPERLINK;
        case HTML:
          return HTML;
        case ONE_TO_MANY:
          throw new MolgenisReaderException(
              "new format doesn't yet support 'ONE_TO_MANY' data type");
        default:
          throw new MolgenisReaderException(
              "new format doesn't yet support " + oldType + " data type");
      }
    } catch (IllegalArgumentException e) {
      throw new MolgenisReaderException("attributes type '" + dataType + "' not known");
    }
  }

  public List<AttributesFileRow> readRowsFromCsv(Reader in) throws MolgenisReaderException {
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
      throw new MolgenisReaderException(e.getMessage());
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

  public List<AttributesFileRow> readRowsFromCsv(File f) throws MolgenisReaderException {
    try {
      return readRowsFromCsv(new FileReader(f));
    } catch (IOException e) {
      throw new MolgenisReaderException(e);
    }
  }
}

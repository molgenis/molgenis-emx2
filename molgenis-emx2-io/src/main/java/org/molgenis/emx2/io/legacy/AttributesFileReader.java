package org.molgenis.emx2.io.legacy;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxType;
import org.molgenis.emx2.beans.EmxColumnBean;
import org.molgenis.emx2.beans.EmxModelBean;
import org.molgenis.emx2.beans.EmxTableBean;
import org.molgenis.emx2.io.MolgenisReaderException;
import org.molgenis.emx2.io.MolgenisReaderMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.EmxType.*;
import static org.molgenis.emx2.io.legacy.AttributesFileHeader.*;

public class AttributesFileReader {

    public EmxModel readModelFromCsv(File f) throws MolgenisReaderException, FileNotFoundException {
        return convertAttributesToModel(readRowsFromCsv(new FileReader(f)));
    }

    public EmxModel readModelFromCsv(Reader in) throws MolgenisReaderException {
        return convertAttributesToModel(readRowsFromCsv(in));
    }

    public EmxModel convertAttributesToModel(List<AttributesFileRow> rows) throws MolgenisReaderException {
        EmxModelBean model = new EmxModelBean();

        int lineNumber = 0;
        List<MolgenisReaderMessage> messages = new ArrayList<>();
        Map<Integer,String> refEntities = new LinkedHashMap<>();
        Map<Integer,EmxColumnBean> refColumns = new LinkedHashMap<>();
        for(AttributesFileRow row: rows) {
            lineNumber++;

            //get or create table
            EmxTableBean table = model.getTable(row.getEntity());
            if(table == null) table = model.addTable(row.getEntity());

            //check if attribute exists
            if(table.getColumn(row.getName()) != null) {
                messages.add(new MolgenisReaderMessage(lineNumber, "attribute "+row.getName()+" is defined twice"));
            }
            else {
                EmxType type = STRING;
                if(row.getDataType() != null) {
                    try {
                        type = convertAttributeTypeToEmxType(row.getDataType());
                    } catch (Exception e) {
                        messages.add(new MolgenisReaderMessage(lineNumber, e.getMessage()));
                    }
                }
                EmxColumnBean column = table.addColumn(row.getName(), type);

                column.setNillable(row.getNillable());
                column.setDescription(row.getDescription());
                column.setReadonly(row.getReadonly());
                column.setValidation(row.getValidationExepression());

                if(column.getType().equals(SELECT) || column.getType().equals(MSELECT) || column.getType().equals(RADIO) || column.getType().equals(CHECKBOX)) {
                    refEntities.put(lineNumber, row.getRefEntity());
                    refColumns.put(lineNumber, column);
                }


//                row.setRefEntity(get(record,REFENTITY));
//                row.setIdAttribute(bool(get(record,IDATTRIBUTE)));
//                row.setRangeMin(integer(get(record,RANGEMIN)));
//                row.setRangeMax(integer(get(record,RANGEMAX)));
//                row.setLabel(get(record,LABEL));
//                row.setAggregateable(bool(get(record,AGGREGATEABLE)));
//                row.setLabelAttribute(bool(get(record,LABELATTRIBUTE)));
//                row.setValidationExepression(get(record,VALIDATIONEXPRESSION));
//                //row.setTags(TAGS);
//                row.setVisibleExpression(get(record,VISIBLE));
//                row.setDefaultValue(get(record,DEFAULTVALUE));
//                row.setPartOfAttribute(get(record,PARTOFATTRIBUTE));
//                row.setExpression(get(record,EXPRESSION));
            }
        }
        //check and set refEntities
        for(Integer line: refEntities.keySet()) {
            String refEntity = refEntities.get(line);
            EmxTable table = model.getTable(refEntity);
            if(table == null) messages.add(new MolgenisReaderMessage(line,"refEntity '"+refEntity+"' is not known"));
            else refColumns.get(line).setRef(table.getIdColumn());

        }
        if(messages.size() > 0) throw new MolgenisReaderException(messages);
        return model;
    }

    private EmxType convertAttributeTypeToEmxType(String dataType) throws MolgenisReaderException {
        try {
            AttributesType oldType = AttributesType.valueOf(dataType.toUpperCase());
            EmxType type = null;
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
                    throw new MolgenisReaderException("new format doesn't yet support 'ONE_TO_MANY' data type");
                default:
                    throw new MolgenisReaderException("new format doesn't yet support " + oldType + " data type");
            }
        } catch(IllegalArgumentException e) {
            throw new MolgenisReaderException("attributes type '"+dataType+"' not known");
        }
    }

    public List<AttributesFileRow> readRowsFromCsv(Reader in) throws MolgenisReaderException {
        try {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(AttributesType.class).withAllowMissingColumnNames().withFirstRecordAsHeader().withIgnoreHeaderCase().withIgnoreSurroundingSpaces().withTrim().withIgnoreEmptyLines(true).parse(in);
            List<AttributesFileRow> rows = new ArrayList<>();
            for (CSVRecord record : records) {
                AttributesFileRow row = new AttributesFileRow();
                row.setEntity(get(record,ENTITY));
                row.setName(get(record,NAME));
                row.setDataType(get(record,DATATYPE));
                row.setRefEntity(get(record,REFENTITY));
                row.setNillable(bool(get(record,NILLABLE)));
                row.setIdAttribute(bool(get(record,IDATTRIBUTE)));
                row.setDescription(get(record,DESCRIPTION));
                row.setRangeMin(integer(get(record,RANGEMIN)));
                row.setRangeMax(integer(get(record,RANGEMAX)));
                row.setLabel(get(record,LABEL));
                row.setAggregateable(bool(get(record,AGGREGATEABLE)));
                row.setLabelAttribute(bool(get(record,LABELATTRIBUTE)));
                row.setReadonly(bool(get(record,READONLY)));
                row.setValidationExepression(get(record,VALIDATIONEXPRESSION));
                //row.setTags(TAGS);
                row.setVisibleExpression(get(record,VISIBLE));
                row.setDefaultValue(get(record,DEFAULTVALUE));
                row.setPartOfAttribute(get(record,PARTOFATTRIBUTE));
                row.setExpression(get(record,EXPRESSION));
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
            if("".equals(value.trim())) return null;
            return value;
        } catch(IllegalArgumentException exception) {
         return null;
        }

    }

    private Integer integer(String value) {
        if(value == null) return null;
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

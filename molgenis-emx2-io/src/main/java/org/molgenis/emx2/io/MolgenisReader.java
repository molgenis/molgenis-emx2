package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.molgenis.emx2.io.format.MolgenisFileHeader;
import org.molgenis.emx2.io.format.MolgenisFileRow;
import org.molgenis.emx2.io.parsers.ColumnDefinition;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxType;
import org.molgenis.emx2.beans.EmxColumnBean;
import org.molgenis.emx2.beans.EmxModelBean;
import org.molgenis.emx2.beans.EmxTableBean;
import org.molgenis.emx2.io.parsers.ColumnDefinitionParser;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MolgenisReader {

    public EmxModel readModelFromCsv(File f) throws IOException {
        return readModelFromCsv(new FileReader(f));
    }

    public EmxModel readModelFromCsv(Reader in) throws IOException {
        List<MolgenisFileRow> rows = readRowsFromCsv(in);
        return convertRowsToModel(rows);
    }

    public List<MolgenisFileRow> readRowsFromCsv(File f) throws IOException {
        return readRowsFromCsv(new FileReader(f));
    }

    public List<MolgenisFileRow> readRowsFromCsv(Reader in) throws MolgenisReaderException {
        try {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(MolgenisFileHeader.class).withFirstRecordAsHeader().withIgnoreHeaderCase().withIgnoreSurroundingSpaces().withTrim().withIgnoreEmptyLines(true).parse(in);
            List<MolgenisFileRow> rows = new ArrayList<>();
            for (CSVRecord record : records) {
                rows.add(new MolgenisFileRow(record.get(MolgenisFileHeader.TABLE), record.get(MolgenisFileHeader.COLUMN), record.get(MolgenisFileHeader.DEFINITION)));
            }
            return rows;
        } catch (IOException e) {
            throw new MolgenisReaderException(e.getMessage());
        }
    }

    public EmxModel convertRowsToModel(List<MolgenisFileRow> rows) throws MolgenisReaderException {

        int line = 0;

        Map<String,EmxTableBean> tables = new LinkedHashMap<>();
        List<MolgenisReaderMessage> messages = new ArrayList<>();

        for(MolgenisFileRow row: rows) {
            line++;

            String tableName = row.getTable();

            if(tableName != null)
            {
                EmxTableBean table = tables.get(tableName);
                if( table == null) {
                    table = new EmxTableBean(tableName);
                    tables.put(tableName, table);
                }

                String columnName = row.getColumn();
                //if table != null and column== null, this definition is for a table
                if(columnName == null) {

                }
                //if table!= null and column != null, this definition if for a column
                else {
                    if(table.getColumn(columnName) != null) {
                        throw new RuntimeException("error on line "+line+": duplicate column definition table='"+tableName+"', column='"+columnName+"'");
                    }

                    List<ColumnDefinition> terms = new ColumnDefinitionParser().parse(line, messages, row.getDefinition());
                    //get type
                    EmxType type = EmxType.STRING;
                    for(ColumnDefinition term: terms) {
                        try {
                            type = EmxType.valueOf(term.name());
                        } catch(Exception e) {
                            //no problem,irrelevant term.
                        }
                    }
                    EmxColumnBean column = table.addColumn(columnName, type);

                }
            }
        }

        return new EmxModelBean(tables);
    }

}

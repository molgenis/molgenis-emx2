package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.molgenis.emx2.io.format.MolgenisFileHeader;
import org.molgenis.emx2.io.format.MolgenisFileRow;
import org.molgenis.emx2.io.parsers.ColumnDefinition;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxType;
import org.molgenis.emx2.io.beans.EmxModelBean;
import org.molgenis.emx2.io.beans.EmxTableBean;
import org.molgenis.emx2.io.parsers.ColumnDefinitionParser;
import org.molgenis.emx2.io.parsers.TableDefinition;
import org.molgenis.emx2.io.parsers.TableDefinitionParser;

import java.io.*;
import java.util.*;

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

        Map<String,EmxTableBean> tables = new LinkedHashMap<>();
        List<MolgenisReaderMessage> messages = new ArrayList<>();

        //first parse attributes
        int line = 0;
        for(MolgenisFileRow row: rows) {
            line++;

            String tableName = row.getTable();
            String columnName = row.getColumn();

            if(!"".equals(tableName) && !"".equals(columnName))
            {
                tables.computeIfAbsent(tableName, k -> new EmxTableBean(tableName));
                EmxTableBean table = tables.get(tableName);

                //if table != null and column== null, this definition is for a table
                //if table != null and column != null, this definition if for a column
                if(columnName != null) {
                    if(table.getColumn(columnName) != null) {
                        throw new MolgenisReaderException("error on line "+line+": duplicate column definition table='"+tableName+"', column='"+columnName+"'");
                    }
                    List<ColumnDefinition> terms = new ColumnDefinitionParser().parse(line, messages, row.getDefinition());
                    table.addColumn(columnName, getType(terms));
                }
            }
        }
        //then parse table
        line = 0;
        for(MolgenisFileRow row: rows) {
            line++;

            String tableName = row.getTable();
            String columnName = row.getColumn();
            if(!"".equals(tableName) && "".equals(columnName)) {
                tables.computeIfAbsent(tableName, k -> new EmxTableBean(tableName));
                EmxTableBean table = tables.get(tableName);
                List<TableDefinition> terms = new TableDefinitionParser().parse(line, messages, row.getDefinition());
                for (TableDefinition term : terms) {
                    switch (term) {
                        case UNIQUE:
                            try {
                                table.addUnique(Arrays.asList(term.getParameterValue().split(",")));
                            } catch(Exception e) {
                                throw new MolgenisReaderException("error on line "+line+": unique parsing failed. "+e.getMessage());
                            }
                    }
                }
            }
        }

        return new EmxModelBean(tables);
    }

    private EmxType getType(List<ColumnDefinition> terms) {
        //get type
        EmxType type = EmxType.STRING;
        for(ColumnDefinition term: terms) {
            try {
                type = EmxType.valueOf(term.name());
            } catch(Exception e) {
                //no problem,irrelevant term.
            }
        }
        return type;
    }

}

package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.molgenis.Column;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.Unique;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.format.EmxDefinitionTerm;
import org.molgenis.emx2.io.format.MolgenisFileRow;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.emx2.io.format.EmxDefinitionTerm.*;

public class MolgenisWriter {

  public void writeCsv(Schema model, Writer writer) throws IOException, MolgenisWriterException {
    CSVPrinter csvPrinter =
        new CSVPrinter(
            writer, CSVFormat.DEFAULT.withHeader("table", "column", "definition", "description"));
    for (MolgenisFileRow row : convertModelToMolgenisFileRows(model)) {
      csvPrinter.printRecord(
          row.getTable(), row.getColumn(), row.getDefinition(), row.getDescription());
    }
    csvPrinter.close();
  }

  public List<MolgenisFileRow> convertModelToMolgenisFileRows(Schema model)
      throws MolgenisWriterException {
    List<MolgenisFileRow> rows = new ArrayList<>();

    for (String tableName : model.getTables()) {
      Table table = model.getTable(tableName);
      if (table.getExtend() != null || !table.getUniques().isEmpty()) {
        rows.add(convertTableToRow(table));
      }
      for (Column column : table.getColumns()) {
        // ignore internal ID
        if (!MOLGENISID.equals(column.getName())) {
          rows.add(convertColumnToRow(table, column));
        }
      }
    }
    return rows;
  }

  private MolgenisFileRow convertColumnToRow(Table table, Column column)
      throws MolgenisWriterException {
    return new MolgenisFileRow(
        table.getName(), column.getName(), getDefinitionString(column), column.getDescription());
  }

  private String getDefinitionString(Column col) throws MolgenisWriterException {
    List<EmxDefinitionTerm> def = new ArrayList<>();

    if (!Column.Type.STRING.equals(col.getType())) {
      EmxDefinitionTerm d = EmxDefinitionTerm.valueOf(col.getType());
      switch (d) {
        case STRING:
          break;
        case INT:
          break;
        case LONG:
          break;
        case BOOL:
          break;
        case DECIMAL:
          break;
        case TEXT:
          break;
        case DATE:
          break;
        case DATETIME:
          break;
        case SELECT:
        case MSELECT:
        case RADIO:
        case CHECKBOX:
          break;
        case NILLABLE:
          break;
        case DEFAULT:
          break;
        case UNIQUE:
          break;
        case READONLY:
          break;
        case VISIBLE:
          break;
        case VALIDATION:
          break;
        case UUID:
          break;
        case HYPERLINK:
          break;
        case EMAIL:
          break;
        case HTML:
          break;
        case FILE:
          break;
        case ENUM:
          break;
        default:
          throw new MolgenisWriterException("unknown type, this is a coding error!");
      }

      def.add(d);
    }
    if (col.isNullable()) def.add(NILLABLE);
    if (col.isReadonly()) def.add(READONLY);
    if (col.getDefaultValue() != null) def.add(DEFAULT.setParameterValue(col.getDefaultValue()));
    if (col.getValidation() != null)
      def.add(VALIDATION.setParameterValue(escapeScript(col.getValidation())));
    if (col.isUnique()) def.add(UNIQUE.setParameterValue(null));
    if (col.getVisible() != null)
      def.add(VISIBLE.setParameterValue(escapeScript(col.getVisible())));

    return join(def, " ");
  }

  private String escapeScript(String value) {
    return value.replace("(", "\\(").replace(")", "\\)");
  }

  private String escapeRef(String value) {
    return value.replace(".", "\\.").replace("(", "\\(").replace(")", "\\)");
  }

  private MolgenisFileRow convertTableToRow(Table table) {
    List<EmxDefinitionTerm> def = new ArrayList<>();
    for (Unique u : table.getUniques()) {
      def.add(UNIQUE.setParameterList(u.getColumnNames()));
    }

    return new MolgenisFileRow(table.getName(), "", join(def, " "));
  }

  private String join(Collection collection, String delimiter) {
    return (String)
        collection.stream().map(Object::toString).collect(Collectors.joining(delimiter));
  }
}

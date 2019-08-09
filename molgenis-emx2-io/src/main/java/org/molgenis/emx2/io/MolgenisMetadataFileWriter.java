package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.molgenis.*;
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

public class MolgenisMetadataFileWriter {

  private MolgenisMetadataFileWriter() {
    // to prevent instantiation of this static method class
  }

  public static void writeCsv(Schema model, Writer writer) throws IOException, MolgenisException {
    CSVPrinter csvPrinter =
        new CSVPrinter(
            writer, CSVFormat.DEFAULT.withHeader("table", "column", "definition", "description"));
    for (MolgenisFileRow row : convertModelToMolgenisFileRows(model)) {
      csvPrinter.printRecord(
          row.getTable(), row.getColumn(), row.getDefinition(), row.getDescription());
    }
    csvPrinter.close();
  }

  public static List<MolgenisFileRow> convertModelToMolgenisFileRows(Schema model)
      throws MolgenisException {
    List<MolgenisFileRow> rows = new ArrayList<>();

    for (String tableName : model.getTableNames()) {
      Table table = model.getTable(tableName);
      if (!table.getUniques().isEmpty()) {
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

  private static MolgenisFileRow convertColumnToRow(Table table, Column column)
      throws MolgenisException {
    return new MolgenisFileRow(
        table.getName(), column.getName(), getDefinitionString(column), column.getDescription());
  }

  private static String getDefinitionString(Column col) throws MolgenisException {
    List<EmxDefinitionTerm> def = new ArrayList<>();

    if (!Type.STRING.equals(col.getType())) {
      EmxDefinitionTerm d = EmxDefinitionTerm.valueOf(col.getType());
      switch (d) {
        case STRING:
          break;
        case INT:
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
        case NILLABLE:
          break;
        case UNIQUE:
          break;
        case UUID:
          break;
        case REF:
          d.setParameterValue(col.getRefTable());
          break;
        case MREF:
          break;
        default:
          throw new MolgenisException("unknown type " + d + " this is a coding error!");
      }

      def.add(d);
    }
    if (col.isNullable()) def.add(NILLABLE);
    if (col.isReadonly()) def.add(READONLY);
    if (col.getDefaultValue() != null) def.add(DEFAULT.setParameterValue(col.getDefaultValue()));
    // if (col.getValidation() != null)
    //  def.add(VALIDATION.setParameterValue(escapeScript(col.getValidation())));
    if (col.isUnique()) def.add(UNIQUE.setParameterValue(null));
    // if (col.getVisible() != null)
    //  def.add(VISIBLE.setParameterValue(escapeScript(col.getVisible())));

    return join(def, " ");
  }

  private static String escapeScript(String value) {
    return value.replace("(", "\\(").replace(")", "\\)");
  }

  private static String escapeRef(String value) {
    return value.replace(".", "\\.").replace("(", "\\(").replace(")", "\\)");
  }

  private static MolgenisFileRow convertTableToRow(Table table) {
    List<EmxDefinitionTerm> def = new ArrayList<>();
    for (Unique u : table.getUniques()) {
      def.add(UNIQUE.setParameterList(u.getColumnNames()));
    }
    return new MolgenisFileRow(table.getName(), "", join(def, " "));
  }

  private static String join(Collection collection, String delimiter) {
    return (String)
        collection.stream().map(Object::toString).collect(Collectors.joining(delimiter));
  }
}

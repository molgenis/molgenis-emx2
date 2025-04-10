package org.molgenis.emx2.typescript;

import static org.molgenis.emx2.utils.TypeUtils.convertToCamelCase;
import static org.molgenis.emx2.utils.TypeUtils.convertToPascalCase;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.molgenis.emx2.*;

public class Generator {

  private static final String FILE_TS_INTERFACE =
      """
export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}
""";

  private static final String ONTOLOGY_TS_INTERFACE =
      """
  export interface ITreeNode {
    name: string;
    children?: ITreeNode[];
    parent?: {
      name: string;
    };
  }

  export interface IOntologyNode extends ITreeNode {
    code?: string;
    definition?: string;
    ontologyTermURI?: string;
    order?: number;
  }
   """;

  public Generator() {}

  public void generate(Schema schema, String fileLocation) {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(fileLocation, "UTF-8");
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    writer.println(
        String.format(
            "// Generated (on: %s) from Generator.java for schema: %s",
            LocalDateTime.now(), schema.getName()));
    writer.println("");

    writer.write(FILE_TS_INTERFACE);
    writer.println("");

    writer.write(ONTOLOGY_TS_INTERFACE);
    writer.println("");

    SchemaMetadata metadata = schema.getMetadata();

    List<TableMetadata> tables = metadata.getTablesIncludingExternal();
    tables.sort(Comparator.comparing(TableMetadata::getTableName));

    for (TableMetadata table : tables) {

      String tableName = convertToPascalCase(table.getTableName());
      writer.println(String.format("export interface I%s {", tableName));

      for (Column column : table.getColumns()) {
        if (column.getColumnType().isHeading()) {
          continue;
        }
        if (column.isSystemColumn()) {
          continue;
        }

        String columnName = convertToCamelCase(column.getName());
        String fieldValue = toTypeScriptInterfaceFieldValue(column);
        String optional = column.isRequired() ? "" : "?";
        writer.println(String.format("  %s%s: %s;", columnName, optional, fieldValue));
      }
      writer.println("}");
      writer.println("");

      writer.println(String.format("export interface I%s_agg {", tableName));
      // todo do all agg
      writer.println("  count: number");
      writer.println("}");
      writer.println("");
    }

    writer.println("");
    writer.flush();
    writer.close();
  }

  private String toTypeScriptInterfaceFieldValue(Column column) {
    ColumnType columnType = column.getColumnType();

    return switch (columnType) {
      case BOOL -> "boolean";
      case BOOL_ARRAY -> "boolean[]";
      case EMAIL, STRING, TEXT, DATE, DATETIME, UUID, AUTO_ID, HYPERLINK, LONG -> "string";
      case EMAIL_ARRAY,
              STRING_ARRAY,
              TEXT_ARRAY,
              DATE_ARRAY,
              DATETIME_ARRAY,
              UUID_ARRAY,
              HYPERLINK_ARRAY,
              LONG_ARRAY ->
          "string[]";
      case INT, DECIMAL -> "number";
      case INT_ARRAY, DECIMAL_ARRAY -> "number[]";
      case REF -> "I" + convertToPascalCase(column.getRefTable().getTableName());
      case REF_ARRAY, REFBACK ->
          "I" + convertToPascalCase(column.getRefTable().getTableName()) + "[]";
      case FILE -> "IFile";
      case ONTOLOGY -> "IOntologyNode";
      case ONTOLOGY_ARRAY -> "IOntologyNode[]";
      default -> "any";
    };
  }
}

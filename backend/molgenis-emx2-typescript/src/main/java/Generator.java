import static org.molgenis.emx2.utils.TypeUtils.convertToCamelCase;
import static org.molgenis.emx2.utils.TypeUtils.convertToPascalCase;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import org.molgenis.emx2.*;

public class Generator {

  private final static String FILE_FIELD_VALUE = """
  export interface IFile {
    id?: string;
    size?: number;
    extension?: string;
    url?: string;
  }
""";

  public Generator() {}

  // "apps/nuxt3-ssr/interfaces/gtypes.ts"
  public void generate(Schema schema, String fileLocation) {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter("gtypes.ts", "UTF-8");
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    SchemaMetadata metadata = schema.getMetadata();
    for (TableMetadata table : metadata.getTables()) {

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
        // String columnType = column.getColumnType().toString();
        String fieldValue = toTypeScriptInterfaceFieldValue(column);
        writer.println(String.format("  %s :%s;", columnName, fieldValue));
      }
      writer.println("}");
      writer.println("");
    }

    writer.println("");
    writer.close();
  }

  private String toTypeScriptInterfaceFieldValue(Column column) {
    ColumnType columnType = column.getColumnType();

    return switch (columnType) {
      case BOOL -> "boolean";
      case BOOL_ARRAY -> "boolean[]";
      case EMAIL, STRING, TEXT, DATE, DATETIME, UUID, AUTO_ID -> "string";
      case EMAIL_ARRAY, STRING_ARRAY, TEXT_ARRAY, DATE_ARRAY, DATETIME_ARRAY, UUID_ARRAY ->
          "string[]";
      case INT, LONG, DECIMAL -> "number";
      case INT_ARRAY, LONG_ARRAY, DECIMAL_ARRAY -> "number[]";
      case REF -> "I" + convertToPascalCase(column.getRefTable().getTableName());
      case REF_ARRAY -> "I" + convertToPascalCase(column.getRefTable().getTableName()) + "[]";
      case FILE -> "IFile";
      case ONTOLOGY ->
      default -> "any";
    };
  }
}
